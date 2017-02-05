package com.github.vuzoll.petrpigbyfield.service

import com.github.vuzoll.petrpigbyfield.domain.job.Job
import com.github.vuzoll.petrpigbyfield.domain.job.JobLog
import com.github.vuzoll.petrpigbyfield.domain.job.JobStatus
import com.github.vuzoll.petrpigbyfield.repository.job.JobRepository
import groovy.util.logging.Slf4j
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.TaskExecutor
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
@Slf4j
class JobsService {

    static final PeriodFormatter TIME_LIMIT_FORMAT = new PeriodFormatterBuilder()
            .appendHours().appendSuffix('h')
            .appendMinutes().appendSuffix('min')
            .appendSeconds().appendSuffix('sec')
            .toFormatter()

    @Autowired
    JobRepository jobRepository

    @Autowired
    TaskExecutor taskExecutor

    @PostConstruct
    void markAbortedJobs() {
        log.info 'Marking all aborted jobs...'
        Collection<Job> activeJobs = findActiveJobs()
        if (activeJobs.empty) {
            log.info 'Found no active jobs'
        } else {
            log.warn "Found ${activeJobs.size()} active jobs to abort"
            activeJobs.each { it.status = JobStatus.ABORTED.toString() }
            jobRepository.save(activeJobs)
        }
    }

    Job getActiveJob() {
        Collection<Job> activeJobs = findActiveJobs()

        if (activeJobs.empty) {
            return null
        }

        if (activeJobs.size() > 1) {
            log.error("There are more than one active job: ${activeJobs}")
            throw new IllegalStateException("There are more than one active job: ${activeJobs}")
        }

        return activeJobs.first()
    }

    Job getJobById(String id) {
        jobRepository.findOne(id)
    }

    Job getLastJob() {
        List<Job> lastJob = jobRepository.findAll(new PageRequest(0, 1, new Sort(Sort.Direction.DESC, 'startTimestamp')))
        if (lastJob.empty) {
            return null
        } else {
            return lastJob.first()
        }
    }

    List<Job> getAllJobs() {
        jobRepository.findAll(new Sort(Sort.Direction.DESC, 'startTimestamp'))
    }

    Job startJob(DurableJob durableJob) {
        Job job = new Job()
        job.name = durableJob.name
        job.status = JobStatus.STARTING.toString()

        job = jobRepository.save job

        Job activeJob = getActiveJob()
        if (activeJob == null) {
            taskExecutor.execute(this.&executeDurableJob.curry(job, durableJob))
        } else {
            job.lastMessage = "There is another active job with id=$activeJob.id, can't accept new one"
            log.warn job.lastMessage
            job.status = JobStatus.ABORTED.toString()
            jobRepository.save job
        }

        return job
    }

    void updateJobStatus(Job job, String updateDelay, String message) {
        job = jobRepository.findOne job.id

        if (job.messageLog == null || job.messageLog.empty || System.currentTimeMillis() - job.messageLog.timestamp.max() > fromDurationString(updateDelay)) {
            log.info "jobId=${job.id}: ${message}"
            JobLog jobLog = new JobLog()
            jobLog.timestamp = System.currentTimeMillis()
            jobLog.time = LocalDateTime.now().toString()
            jobLog.message = message

            job.lastMessage = message
            job.lastUpdateTime = jobLog.time
            job.timeTaken = toDurationString(jobLog.timestamp - job.startTimestamp)
            job.messageLog = job.messageLog == null ? [ jobLog ] : job.messageLog + jobLog

            jobRepository.save job
        } else {
            log.debug "jobId=${job.id}: ${message}"
        }
    }

    private void executeDurableJob(Job job, DurableJob durableJob) {
        try {
            log.info "jobId=${job.id}: starting..."
            job.startTimestamp = System.currentTimeMillis()
            job.startTime = LocalDateTime.now().toString()
            job.lastUpdateTime = job.startTime
            job.timeTaken = '0sec'
            job.status = JobStatus.RUNNING.toString()
            job = jobRepository.save job

            while (true) {
                log.info "jobId=${job.id}: already last ${toDurationString(System.currentTimeMillis() - job.startTimestamp)}"

                job = jobRepository.findOne job.id

                if (job.status == JobStatus.STOPPING.toString()) {
                    job.lastMessage = 'stopped by client request'
                    log.info "jobId=${job.id}: ${job.lastMessage}"
                    break
                }

                if (durableJob.finished) {
                    job.lastMessage = 'finished successfully'
                    log.info "jobId=${job.id}: ${job.lastMessage}"
                    break
                }

                durableJob.doSomething(this.&updateJobStatus.curry(job))
            }

            log.info "jobId=${job.id}: succeeded"
            job.endTime = LocalDateTime.now().toString()
            job.lastUpdateTime = job.endTime
            job.timeTaken = toDurationString(System.currentTimeMillis() - job.startTimestamp)
            job.status = JobStatus.COMPLETED.toString()
            jobRepository.save jobRepository
        } catch (e) {
            log.error("jobId=${job.id}: failed", e)
            job.lastMessage = "Failed because of ${e.class.name}, with lastMessage: ${e.message}"
            job.endTime = LocalDateTime.now().toString()
            job.lastUpdateTime = job.endTime
            job.timeTaken = toDurationString(System.currentTimeMillis() - job.startTimestamp)
            job.status = JobStatus.FAILED.toString()
            jobRepository.save jobRepository
        }
    }

    private Collection<Job> findActiveJobs() {
        jobRepository.findByStatus(JobStatus.RUNNING.toString()) + jobRepository.findByStatus(JobStatus.STOPPING.toString())
    }

    static String toDurationString(long duration) {
        String durationString = TIME_LIMIT_FORMAT.print(new Period(duration))
        if (durationString.empty) {
            return '0sec'
        } else {
            return durationString
        }
    }

    static long fromDurationString(String durationAsString) {
        TIME_LIMIT_FORMAT.parsePeriod(durationAsString).toStandardDuration().getStandardSeconds() * 1000
    }
}

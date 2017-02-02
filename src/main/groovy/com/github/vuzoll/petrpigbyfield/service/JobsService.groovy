package com.github.vuzoll.petrpigbyfield.service

import com.github.vuzoll.petrpigbyfield.domain.job.Job
import com.github.vuzoll.petrpigbyfield.domain.job.JobStatus
import com.github.vuzoll.petrpigbyfield.repository.job.JobRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.TaskExecutor
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Service
@Slf4j
class JobsService {

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
            log.warn "Found ${activeJobs.size()} active jobs to abortr"
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

        Job activeJob = getActiveJob()
        if (activeJob == null) {
            job.status = JobStatus.RUNNING.toString()
            executeDurableJob(durableJob)
        } else {
            job.message = "There is another active job with id=$activeJob.id, can't accept new one"
            log.warn job.message
            job.status = JobStatus.ABORTED.toString()
        }

        jobRepository.save job

        return job
    }

    private void executeDurableJob(DurableJob durableJob) {
        assert null : 'not implemented yet'
    }

    private Collection<Job> findActiveJobs() {
        jobRepository.findByStatus(JobStatus.RUNNING.toString()) + jobRepository.findByStatus(JobStatus.STOPPING.toString())
    }
}

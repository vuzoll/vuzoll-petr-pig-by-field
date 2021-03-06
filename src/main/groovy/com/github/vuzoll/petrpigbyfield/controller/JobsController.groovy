package com.github.vuzoll.petrpigbyfield.controller

import com.github.vuzoll.petrpigbyfield.domain.job.Job
import com.github.vuzoll.petrpigbyfield.service.JobsService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j
class JobsController {

    @Autowired
    JobsService jobsService

    @GetMapping(path = '/job/{jobId}')
    @ResponseBody Job getJobById(@PathVariable String jobId) {
        Job job = jobsService.getJobById(jobId)

        if (job == null) {
            return null
        }

        if (job.messageLog != null) {
            job.messageLog = job.messageLog.sort({ -it.timestamp }).take(20)
        } else {
            job.messageLog = []
        }

        return job
    }

    @GetMapping(path = '/job/last')
    @ResponseBody Job getLastJob() {
        Job job = jobsService.getLastJob()

        if (job == null) {
            return null
        }

        if (job.messageLog != null) {
            job.messageLog = job.messageLog.sort({ -it.timestamp }).take(20)
        } else {
            job.messageLog = []
        }

        return job
    }

    @GetMapping(path = '/job/active')
    @ResponseBody Job getActiveJob() {
        Job job = jobsService.getActiveJob()

        if (job == null) {
            return null
        }

        if (job.messageLog != null) {
            job.messageLog = job.messageLog.sort({ -it.timestamp }).take(20)
        } else {
            job.messageLog = []
        }

        return job
    }

    @GetMapping(path = '/job')
    @ResponseBody List<Job> getAllJobs() {
        List<Job> allJobs = jobsService.getAllJobs()

        allJobs.each { Job job ->
            if (job.messageLog != null) {
                job.messageLog = job.messageLog.sort({ -it.timestamp }).take(20)
            } else {
                job.messageLog = []
            }
        }

        return allJobs
    }
}

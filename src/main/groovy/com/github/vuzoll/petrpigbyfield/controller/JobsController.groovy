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
        jobsService.getJobById(jobId)
    }

    @GetMapping(path = '/job/last')
    @ResponseBody Job getLastJob() {
        jobsService.getLastJob()
    }

    @GetMapping(path = '/job/active')
    @ResponseBody Job getActiveJob() {
        jobsService.getActiveJob()
    }

    @GetMapping(path = '/job')
    @ResponseBody List<Job> getAllJobs() {
        jobsService.getAllJobs()
    }
}

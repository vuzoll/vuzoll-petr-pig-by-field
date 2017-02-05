package com.github.vuzoll.petrpigbyfield.controller

import com.github.vuzoll.petrpigbyfield.domain.job.Job
import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import com.github.vuzoll.petrpigbyfield.repository.vk.VkFacultyRepository
import com.github.vuzoll.petrpigbyfield.service.FacultiesListService
import com.github.vuzoll.petrpigbyfield.service.JobsService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j
class AssignFieldsToFacultiesController {

    @Autowired
    JobsService jobsService

    @Autowired
    FacultiesListService facultiesListService

    @Autowired
    VkFacultyRepository vkFacultyRepository

    @PostMapping(path = '/prepare/faculties')
    @ResponseBody Job prepareFacultiesList() {
        log.info "Receive request to prepare faculties list"

        return jobsService.startJob(facultiesListService.prepareFacultiesListJob())
    }

    @GetMapping(path = '/faculty')
    @ResponseBody List<VkFaculty> getAllFaculties() {
        vkFacultyRepository.findAll(new Sort(new Sort.Order(Sort.Direction.DESC, 'numberOfProfiles')))
    }

    @GetMapping(path = '/faculty/{facultyId}')
    @ResponseBody VkFaculty getFacultyById(@PathVariable String facultyId) {
        vkFacultyRepository.findOne(facultyId)
    }
}

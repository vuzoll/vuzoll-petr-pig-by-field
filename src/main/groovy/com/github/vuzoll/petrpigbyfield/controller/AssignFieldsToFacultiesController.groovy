package com.github.vuzoll.petrpigbyfield.controller

import com.github.vuzoll.petrpigbyfield.domain.Field
import com.github.vuzoll.petrpigbyfield.domain.job.Job
import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import com.github.vuzoll.petrpigbyfield.repository.FieldRepository
import com.github.vuzoll.petrpigbyfield.repository.vk.VkFacultyRepository
import com.github.vuzoll.petrpigbyfield.service.FacultiesListService
import com.github.vuzoll.petrpigbyfield.service.FieldService
import com.github.vuzoll.petrpigbyfield.service.JobsService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
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

    @Autowired
    FieldRepository fieldRepository

    @Autowired
    FieldService fieldService

    @PostMapping(path = '/prepare/faculties')
    @ResponseBody Job prepareFacultiesList() {
        log.info "Receive request to prepare faculties list"

        return jobsService.startJob(facultiesListService.prepareFacultiesListJob())
    }

    @PostMapping(path = '/prepare/fields')
    @ResponseBody Job prepareFieldsList() {
        log.info "Receive request to prepare fields list"

        return jobsService.startJob(fieldService.prepareFieldsListJob())
    }

    @GetMapping(path = '/faculty/orderedByNumberOfProfiles')
    @ResponseBody List<VkFaculty> getAllFacultiesOrderedByNumberOfProfiles() {
        vkFacultyRepository.findAll(new Sort(new Sort.Order(Sort.Direction.DESC, 'numberOfProfiles')))
    }

    @GetMapping(path = '/faculty/orderedByNumberOfProfiles/{indexByNumberOfProfiles}')
    @ResponseBody VkFaculty getFacultyByIndexByNumberOfProfiles(@PathVariable Integer indexByNumberOfProfiles) {
        facultyByIndexByNumberOfProfiles(indexByNumberOfProfiles)
    }

    @PostMapping(path = '/faculty/orderedByNumberOfProfiles/{indexByNumberOfProfiles}/field/{field}')
    @ResponseBody VkFaculty assignFieldToFaculty(@PathVariable Integer indexByNumberOfProfiles, String field) {
        VkFaculty faculty = facultyByIndexByNumberOfProfiles(indexByNumberOfProfiles)
        faculty.field = field

        log.info "Receive request to assign field ${field} to faculty id=${faculty.id}, name=${faculty.facultyName} ${faculty.university.universityName}"

        vkFacultyRepository.save faculty
    }

    @GetMapping(path = '/field/orderedByNumberOfProfiles')
    @ResponseBody List<Field> getAllFieldsOrderedByNumberOfProfiles() {
        fieldRepository.findAll(new Sort(new Sort.Order(Sort.Direction.DESC, 'numberOfProfiles')))
    }

    @GetMapping(path = '/field/orderedByNumberOfProfiles/{indexByNumberOfProfiles}')
    @ResponseBody Field getFieldByIndexByNumberOfProfiles(@PathVariable Integer indexByNumberOfProfiles) {
        fieldRepository.findAll(new PageRequest(indexByNumberOfProfiles - 1, 1, new Sort(new Sort.Order(Sort.Direction.DESC, 'numberOfProfiles')))).content.first()
    }

    private VkFaculty facultyByIndexByNumberOfProfiles(Integer indexByNumberOfProfiles) {
        vkFacultyRepository.findAll(new PageRequest(indexByNumberOfProfiles - 1, 1, new Sort(new Sort.Order(Sort.Direction.DESC, 'numberOfProfiles')))).content.first()
    }
}

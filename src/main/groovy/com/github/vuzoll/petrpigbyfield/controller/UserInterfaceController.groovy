package com.github.vuzoll.petrpigbyfield.controller

import com.github.vuzoll.petrpigbyfield.domain.Field
import com.github.vuzoll.petrpigbyfield.domain.job.Job
import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import com.github.vuzoll.petrpigbyfield.repository.FieldRepository
import com.github.vuzoll.petrpigbyfield.repository.vk.VkFacultyRepository
import com.github.vuzoll.petrpigbyfield.service.FacultiesListService
import com.github.vuzoll.petrpigbyfield.service.FieldService
import com.github.vuzoll.petrpigbyfield.service.JobsService
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletResponse

@RestController
@Slf4j
class UserInterfaceController {

    static final Sort SORT_BY_NUMBER_OF_PROFILES_DESC = new Sort(new Sort.Order(Sort.Direction.DESC, 'numberOfProfiles'))

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

    @GetMapping(path = '/ui/prepare/faculties')
    @ResponseBody String prepareFacultiesList() {
        log.info "Receive request to prepare faculties list"

        Job job = jobsService.startJob(facultiesListService.prepareFacultiesListJob())

        return "/job/${job.id}"
    }

    @GetMapping(path = '/ui/prepare/fields')
    @ResponseBody String prepareFieldsList() {
        log.info "Receive request to prepare fields list"

        Job job = jobsService.startJob(fieldService.prepareFieldsListJob())

        return "/job/${job.id}"
    }

    @GetMapping(path = '/ui/faculty')
    void getAllFacultiesOrderedByNumberOfProfiles(HttpServletResponse response) {
        response.outputStream.withPrintWriter { writer ->
            vkFacultyRepository.findAll(SORT_BY_NUMBER_OF_PROFILES_DESC).collect(this.&toFacultyPresentation).each( { writer.write("${it}\n") } )
        }
    }

    @GetMapping(path = '/ui/faculty/{indexByNumberOfProfiles}')
    void getFacultyByIndexByNumberOfProfiles(@PathVariable Integer indexByNumberOfProfiles, HttpServletResponse response) {
        response.outputStream.withPrintWriter { writer ->
            writer.write("${toFacultyPresentation(facultyByIndexByNumberOfProfiles(indexByNumberOfProfiles))}\n")
        }
    }

    @GetMapping(path = '/ui/faculty/{indexByNumberOfProfiles}/{field}')
    void assignFieldToFaculty(@PathVariable Integer indexByNumberOfProfiles, @PathVariable String field, HttpServletResponse response) {
        VkFaculty faculty = facultyByIndexByNumberOfProfiles(indexByNumberOfProfiles)
        faculty.field = field

        log.info "Receive request to assign field ${field} to faculty id=${faculty.id}, name=${faculty.facultyName} ${faculty.university.universityName}"

        response.outputStream.withPrintWriter { writer ->
            writer.write("${toFacultyPresentation(vkFacultyRepository.save(faculty))}\n")
        }
    }

    @GetMapping(path = '/ui/faculty/unassigned')
    void getFirstUnassignedFaculty(HttpServletResponse response) {
        response.outputStream.withPrintWriter { writer ->
            vkFacultyRepository.findAll(SORT_BY_NUMBER_OF_PROFILES_DESC).find({ StringUtils.isBlank(it.field) }).collect(this.&toFacultyPresentation).each( { writer.write("${it}\n") } )
        }
    }

    @GetMapping(path = '/ui/faculty/unassigned/{field}')
    void assignFieldToFirstUnassignedFaculty(@PathVariable String field, HttpServletResponse response) {
        VkFaculty faculty = vkFacultyRepository.findAll(SORT_BY_NUMBER_OF_PROFILES_DESC).find({ StringUtils.isBlank(it.field) })
        faculty.field = field

        log.info "Receive request to assign field ${field} to faculty id=${faculty.id}, name=${faculty.facultyName} ${faculty.university.universityName}"

        response.outputStream.withPrintWriter { writer ->
            writer.write("${toFacultyPresentation(vkFacultyRepository.save(faculty))}\n")
        }
    }

    @GetMapping(path = '/ui/field')
    void getAllFieldsOrderedByNumberOfProfiles(HttpServletResponse response) {
        jobsService.startJobAndWaitForFinish(fieldService.prepareFieldsListJob())

        response.outputStream.withPrintWriter { writer ->
            fieldRepository.findAll(SORT_BY_NUMBER_OF_PROFILES_DESC).collect(this.&toFieldPresentation).each( { writer.write("${it}\n") } )
        }
    }

    @GetMapping(path = '/ui/field/{indexByNumberOfProfiles}')
    void getFieldByIndexByNumberOfProfiles(@PathVariable Integer indexByNumberOfProfiles, HttpServletResponse response) {
        jobsService.startJobAndWaitForFinish(fieldService.prepareFieldsListJob())

        response.outputStream.withPrintWriter { writer ->
            writer.write("${toFieldPresentation(fieldByIndexByNumberOfProfiles(indexByNumberOfProfiles))}\n")
        }
    }

    @GetMapping(path = '/ui/field/{indexByNumberOfProfiles}/{newFieldName}')
    void renameField(@PathVariable Integer indexByNumberOfProfiles, @PathVariable String newFieldName, HttpServletResponse response) {
        jobsService.startJobAndWaitForFinish(fieldService.prepareFieldsListJob())

        Field oldField = fieldByIndexByNumberOfProfiles(indexByNumberOfProfiles)
        Collection<VkFaculty> fieldFaculties = vkFacultyRepository.findByField(oldField.name)
        fieldFaculties.each { it.field = newFieldName }
        vkFacultyRepository.save(fieldFaculties)

        jobsService.startJobAndWaitForFinish(fieldService.prepareFieldsListJob())

        response.outputStream.withPrintWriter { writer ->
            writer.write("${toFieldPresentation(fieldByIndexByNumberOfProfiles(indexByNumberOfProfiles))}\n")
        }
    }

    private VkFaculty facultyByIndexByNumberOfProfiles(Integer indexByNumberOfProfiles) {
        vkFacultyRepository.findAll(new PageRequest(indexByNumberOfProfiles - 1, 1, SORT_BY_NUMBER_OF_PROFILES_DESC)).content.first()
    }

    private Field fieldByIndexByNumberOfProfiles(Integer indexByNumberOfProfiles) {
        fieldRepository.findAll(new PageRequest(indexByNumberOfProfiles - 1, 1, SORT_BY_NUMBER_OF_PROFILES_DESC)).content.first()
    }

    private String toFacultyPresentation(VkFaculty faculty) {
        Integer indexByNumberOfProfiles = vkFacultyRepository.findAll(SORT_BY_NUMBER_OF_PROFILES_DESC).findIndexOf({ it == faculty }) + 1

        String facultyName = StringUtils.isNoneBlank(faculty.facultyName) ? faculty.facultyName : "faculty_id=${faculty.facultyId}"
        String universityName = StringUtils.isNoneBlank(faculty.university.universityName) ? faculty.university.universityName : "university_id=${faculty.university.universityId}"
        String numberOfProfiles = "${faculty.numberOfProfiles} профилей" ?: 'количество профилей неизвестно'
        String field = StringUtils.isNoneBlank(faculty.field) ? faculty.field : 'сфера не отмечена'

        return "#${indexByNumberOfProfiles}: ${facultyName} ${universityName} (${field}) - $numberOfProfiles"
    }

    private String toFieldPresentation(Field field) {
        String fieldName = field.name
        String numberOfProfiles = "${field.numberOfProfiles} профилей" ?: 'количество профилей неизвестно'
        List<String> facultiesPresentation = field.faculties.sort({ -it.numberOfProfiles }).collect(this.&toFacultyPresentation)

        return "${fieldName} - $numberOfProfiles\n${facultiesPresentation.collect({"\t\t${it}"}).join('\n')}"
    }
}

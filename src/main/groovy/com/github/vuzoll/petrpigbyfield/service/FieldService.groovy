package com.github.vuzoll.petrpigbyfield.service

import com.github.vuzoll.petrpigbyfield.domain.Field
import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import com.github.vuzoll.petrpigbyfield.repository.vk.VkFacultyRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Slf4j
class FieldService {

    @Autowired
    VkFacultyRepository vkFacultyRepository

    List<VkFaculty> getFacultiesByField(String field) {
        vkFacultyRepository.findAll().findAll({ VkFaculty faculty -> faculty.field == field })
    }

    List<Field> getAllFacultiesByField() {
        Collection<VkFaculty> allFaculties = vkFacultyRepository.findAll()

        allFaculties.field.collect { String fieldName ->
            List<VkFaculty> faculties = allFaculties.findAll({ VkFaculty faculty -> faculty.field == fieldName })

            Field.builder()
                    .name(fieldName)
                    .faculties(faculties)
                    .numberOfProfiles(faculties.numberOfProfiles.sum())
                    .build()
        }
    }
}

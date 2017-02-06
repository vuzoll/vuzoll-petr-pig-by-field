package com.github.vuzoll.petrpigbyfield.service

import com.github.vuzoll.petrpigbyfield.domain.Field
import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import com.github.vuzoll.petrpigbyfield.repository.FieldRepository
import com.github.vuzoll.petrpigbyfield.repository.vk.VkFacultyRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Slf4j
class FieldService {

    static final String FIELDS_LIST_UPDATE_DELAY = System.getenv('FIELDS_LIST_UPDATE_DELAY') ?: '5sec'

    @Autowired
    VkFacultyRepository vkFacultyRepository

    @Autowired
    FieldRepository fieldRepository

    DurableJob prepareFieldsListJob() {
        new DurableJob('prepare fields list') {

            @Override
            void doSomething(Closure statusUpdater) {
                prepareFieldsList(statusUpdater.curry(FIELDS_LIST_UPDATE_DELAY))
                markFinished()
            }
        }
    }

    void prepareFieldsList(Closure statusUpdater) {
        statusUpdater.call('getting list of all faculties')
        Collection<VkFaculty> allFaculties = vkFacultyRepository.findAll()

        statusUpdater.call("getting list of all fields among ${allFaculties.size()} faculties")
        Collection<String> allFieldNames = allFaculties.field

        statusUpdater.call("cleaning all previously prepared field records from database")
        fieldRepository.deleteAll()

        allFieldNames.eachWithIndex { String fieldName, int index ->
            statusUpdater.call("processing field ${fieldName} which is ${index} out of ${allFieldNames.size()}")

            List<VkFaculty> faculties = allFaculties.findAll({ VkFaculty faculty -> faculty.field == fieldName })

            Field field = Field.builder()
                    .name(fieldName)
                    .faculties(faculties)
                    .numberOfProfiles(faculties.numberOfProfiles.sum() as Integer)
                    .build()

            fieldRepository.save field
        }
    }
}

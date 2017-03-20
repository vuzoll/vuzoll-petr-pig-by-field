package com.github.vuzoll.petrpigbyfield.service

import com.github.vuzoll.petrpigbyfield.domain.Field
import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import com.github.vuzoll.petrpigbyfield.repository.FieldRepository
import com.github.vuzoll.petrpigbyfield.repository.vk.VkFacultyRepository
import groovy.util.logging.Slf4j
import io.github.yermilov.kerivnyk.service.DurableJob
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Slf4j
class FieldService {

    @Autowired
    VkFacultyRepository vkFacultyRepository

    @Autowired
    FieldRepository fieldRepository

    DurableJob prepareFieldsListJob(String datasetName) {
        new DurableJob("prepare fields list for dataset ${datasetName}") {

            @Override
            void act() {
                log.info "getting list of all faculties for dataset=${datasetName}"
                Collection<VkFaculty> allFaculties = vkFacultyRepository.findByDatasetName(datasetName)

                log.info "getting list of all fields among ${allFaculties.size()} faculties for dataset=${datasetName}"
                Collection<String> allFieldNames = allFaculties.field.findAll({ it != null }).unique()

                log.info "cleaning all previously prepared field records from database for dataset=${datasetName}"
                fieldRepository.deleteByDatasetName(datasetName)

                allFieldNames.eachWithIndex { String fieldName, int index ->
                    log.info "processing field ${fieldName} which is ${index} out of ${allFieldNames.size()} for dataset=${datasetName}"

                    List<VkFaculty> faculties = allFaculties.findAll({ VkFaculty faculty -> faculty.field == fieldName })

                    Field field = Field.builder()
                            .name(fieldName)
                            .datasetName("${datasetName}-fields")
                            .faculties(faculties)
                            .numberOfProfiles(faculties.numberOfProfiles.sum() as Integer)
                            .build()

                    fieldRepository.save field
                }

                finished()
            }
        }
    }
}

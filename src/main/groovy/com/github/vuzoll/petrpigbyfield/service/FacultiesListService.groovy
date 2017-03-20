package com.github.vuzoll.petrpigbyfield.service

import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import com.github.vuzoll.petrpigbyfield.domain.vk.VkProfile
import com.github.vuzoll.petrpigbyfield.domain.vk.VkUniversity
import com.github.vuzoll.petrpigbyfield.repository.vk.VkFacultyRepository
import com.github.vuzoll.petrpigbyfield.repository.vk.VkProfileRepository
import groovy.util.logging.Slf4j
import io.github.yermilov.kerivnyk.service.DurableJob
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

import static org.springframework.data.mongodb.core.query.Criteria.where
import static org.springframework.data.mongodb.core.query.Query.query

@Service
@Slf4j
class FacultiesListService {

    static final Integer UKRAINE_ID = 2

    @Autowired
    MongoTemplate mongoTemplate

    @Autowired
    VkFacultyRepository vkFacultyRepository

    @Autowired
    VkProfileRepository vkProfileRepository

    DurableJob prepareFacultiesListJob(String datasetName) {
        new DurableJob("prepare faculties list for dataset=${datasetName}") {

            @Override
            void act() {
                final long totalProfileCount = vkProfileRepository.countByDatasetName(datasetName)

                log.info "determining universities location for dataset=${datasetName}"
                Map<VkUniversity, Integer> ukrainianUniversityBalance = [:]
                mongoTemplate.stream(query(where('datasetName').is(datasetName)), VkProfile).eachWithIndex { VkProfile vkProfile, int index ->
                    log.info "processing profile ${index} / ${totalProfileCount} for determining universities location for dataset=${datasetName}"
                    vkProfile.universityRecords.collect(VkFaculty.&fromVkUniversityRecord).findAll({ it != null }).each { VkFaculty faculty ->
                        int balanceDelta = faculty.university.countryId == UKRAINE_ID ? 1 : -1
                        ukrainianUniversityBalance.put(faculty.university, ukrainianUniversityBalance.getOrDefault(faculty.university, 0) + balanceDelta)
                    }
                }

                log.info "preparing faculties list for dataset=${datasetName}"
                Map<VkFaculty, Integer> facultiesDistribution = [:]
                mongoTemplate.stream(query(where('datasetName').is(datasetName)), VkProfile).eachWithIndex { VkProfile vkProfile, int index ->
                    log.info "processing profile ${index} / ${totalProfileCount} for preparing faculties list for dataset=${datasetName}"
                    vkProfile.universityRecords.collect(VkFaculty.&fromVkUniversityRecord).findAll(this.&isUkrainian.curry(ukrainianUniversityBalance)).each { VkFaculty faculty ->
                        facultiesDistribution.put(faculty, facultiesDistribution.get(faculty, 0) + 1)
                    }
                }

                facultiesDistribution.each({ VkFaculty vkFaculty, Integer numberOfProfiles ->
                    vkFaculty.datasetName = "${datasetName}-faculties"
                    vkFaculty.numberOfProfiles = numberOfProfiles
                    vkFaculty.field = vkFacultyRepository.findOneByFacultyId(vkFaculty.facultyId)?.field
                })

                log.info "cleaning all previously prepared faculty records from database for dataset=${datasetName}"
                vkFacultyRepository.deleteByDatasetName(datasetName)

                log.info "saving ${facultiesDistribution.keySet().size()} faculty record to database for dataset=${datasetName}"
                vkFacultyRepository.save facultiesDistribution.keySet()

                finished()
            }

            private boolean isUkrainian(Map<VkUniversity, Integer> ukrainianUniversityBalance, VkFaculty faculty) {
                faculty != null && ukrainianUniversityBalance.getOrDefault(faculty.university, 0) > 0
            }
        }
    }
}

package com.github.vuzoll.petrpigbyfield.service

import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import com.github.vuzoll.petrpigbyfield.domain.vk.VkProfile
import com.github.vuzoll.petrpigbyfield.domain.vk.VkUniversity
import com.github.vuzoll.petrpigbyfield.domain.vk.VkUniversityRecord
import com.github.vuzoll.petrpigbyfield.repository.vk.VkFacultyRepository
import com.github.vuzoll.petrpigbyfield.repository.vk.VkProfileRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class FacultiesListService {

    static final String FACULTIES_LIST_UPDATE_DELAY = System.getenv('FACULTIES_LIST_UPDATE_DELAY') ?: '1min'

    static final Integer UKRAINE_ID = 2

    @Autowired
    MongoTemplate mongoTemplate

    @Autowired
    VkFacultyRepository vkFacultyRepository

    @Autowired
    VkProfileRepository vkProfileRepository

    DurableJob prepareFacultiesListJob() {
        new DurableJob('prepare faculties list') {

            @Override
            void doSomething(Closure statusUpdater) {
                prepareFacultiesList(statusUpdater.curry(FACULTIES_LIST_UPDATE_DELAY))
                markFinished()
            }
        }
    }

    private void prepareFacultiesList(Closure statusUpdater) {
        Map<VkFaculty, Integer> facultiesDistribution = [:]

        final long totalProfileCount = vkProfileRepository.count()

        mongoTemplate.stream(new Query(), VkProfile).eachWithIndex { VkProfile vkProfile, int index ->
            statusUpdater.call("processing profile ${index} / ${totalProfileCount}")
            vkProfile.universityRecords.collect(VkFaculty.&fromVkUniversityRecord).findAll({ it != null && it.university.countryId == UKRAINE_ID }).each { VkFaculty faculty ->
                facultiesDistribution.put(faculty, facultiesDistribution.get(faculty, 0) + 1)
            }
        }

        facultiesDistribution.each({ VkFaculty vkFaculty, Integer numberOfProfiles ->
            vkFaculty.numberOfProfiles = numberOfProfiles
            vkFaculty.field = vkFacultyRepository.findOneByFacultyId(vkFaculty.facultyId)?.field
        })

        statusUpdater.call("cleaning all previously prepared faculty records from database")
        vkFacultyRepository.deleteAll()

        statusUpdater.call("saving ${facultiesDistribution.keySet().size()} faculty record to database")
        vkFacultyRepository.save facultiesDistribution.keySet()
    }
}

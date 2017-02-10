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
        final long totalProfileCount = vkProfileRepository.count()

        statusUpdater.call("determining universities location")
        Map<VkUniversity, Integer> ukrainianUniversityBalance = [:]
        mongoTemplate.stream(new Query(), VkProfile).eachWithIndex { VkProfile vkProfile, int index ->
            statusUpdater.call("processing profile ${index} / ${totalProfileCount} for determining universities location")
            vkProfile.universityRecords.collect(VkFaculty.&fromVkUniversityRecord).findAll({ it != null }).each { VkFaculty faculty ->
                int balanceDelta = faculty.university.countryId == UKRAINE_ID ? 1 : -1
                ukrainianUniversityBalance.put(faculty.university, ukrainianUniversityBalance.getOrDefault(faculty.university, 0) + balanceDelta)
            }
        }

        statusUpdater.call("preparing faculties list")
        Map<VkFaculty, Integer> facultiesDistribution = [:]
        mongoTemplate.stream(new Query(), VkProfile).eachWithIndex { VkProfile vkProfile, int index ->
            statusUpdater.call("processing profile ${index} / ${totalProfileCount} for preparing faculties list")
            vkProfile.universityRecords.collect(VkFaculty.&fromVkUniversityRecord).findAll(this.&isUkrainian.curry(ukrainianUniversityBalance)).each { VkFaculty faculty ->
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

    private boolean isUkrainian(Map<VkUniversity, Integer> ukrainianUniversityBalance, VkFaculty faculty) {
        faculty != null && ukrainianUniversityBalance.getOrDefault(faculty.university, 0) > 0
    }
}

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
            vkProfile.universityRecords.collect(this.&toFaculty).findAll({ it != null && it.university.countryId == UKRAINE_ID }).each { VkFaculty faculty ->
                facultiesDistribution.put(faculty, facultiesDistribution.get(faculty, 0) + 1)
            }
        }

        facultiesDistribution.each({ VkFaculty vkFaculty, Integer numberOfProfiles -> vkFaculty.numberOfProfiles = numberOfProfiles})

        statusUpdater.call("cleaning all previously prepared faculty records from database")
        vkFacultyRepository.deleteAll()

        statusUpdater.call("saving ${facultiesDistribution.keySet().size()} faculty record to database")
        vkFacultyRepository.save facultiesDistribution.keySet()
    }

    private VkFaculty toFaculty(VkUniversityRecord vkUniversityRecord) {
        VkUniversity university = toUniversity(vkUniversityRecord)
        if (university == null) {
            return null
        }
        if (vkUniversityRecord.facultyId == null) {
            return null
        }

        return VkFaculty.builder()
                .university(university)
                .facultyId(vkUniversityRecord.facultyId)
                .facultyName(vkUniversityRecord.facultyName)
                .build()
    }

    private VkUniversity toUniversity(VkUniversityRecord vkUniversityRecord) {
        if (vkUniversityRecord.universityId == null) {
            return null
        }
        if (vkUniversityRecord.countryId == null) {
            return null
        }

        return VkUniversity.builder()
                .universityId(vkUniversityRecord.universityId)
                .universityName(vkUniversityRecord.universityName)
                .countryId(vkUniversityRecord.countryId)
                .cityId(vkUniversityRecord.cityId)
                .build()
    }
}

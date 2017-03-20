package com.github.vuzoll.petrpigbyfield.domain.vk

import groovy.transform.EqualsAndHashCode
import groovy.transform.builder.Builder
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed

@EqualsAndHashCode(includes = 'facultyId')
@Builder
class VkFaculty {

    @Id
    String id

    @Indexed
    String datasetName

    VkUniversity university
    Integer facultyId
    String facultyName
    Integer graduationYear

    Integer numberOfProfiles

    String field

    static VkFaculty fromVkUniversityRecord(VkUniversityRecord vkUniversityRecord) {
        VkUniversity university = VkUniversity.fromVkUniversityRecord(vkUniversityRecord)
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
                .graduationYear(vkUniversityRecord.graduationYear)
                .build()
    }
}

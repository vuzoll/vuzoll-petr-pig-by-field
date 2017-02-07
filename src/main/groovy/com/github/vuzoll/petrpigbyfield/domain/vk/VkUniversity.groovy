package com.github.vuzoll.petrpigbyfield.domain.vk

import groovy.transform.EqualsAndHashCode
import groovy.transform.builder.Builder

@EqualsAndHashCode(includes = 'universityId')
@Builder
class VkUniversity {

    Integer universityId
    Integer countryId
    Integer cityId
    String universityName

    static VkUniversity fromVkUniversityRecord(VkUniversityRecord vkUniversityRecord) {
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

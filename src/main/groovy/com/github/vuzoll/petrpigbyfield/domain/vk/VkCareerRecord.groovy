package com.github.vuzoll.petrpigbyfield.domain.vk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class VkCareerRecord {

    Integer groupId
    Integer countryId
    Integer cityId
    Integer from
    Integer until
    String position
}

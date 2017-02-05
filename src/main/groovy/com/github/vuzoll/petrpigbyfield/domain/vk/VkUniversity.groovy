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
}

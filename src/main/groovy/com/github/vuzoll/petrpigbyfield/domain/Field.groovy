package com.github.vuzoll.petrpigbyfield.domain

import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import groovy.transform.EqualsAndHashCode
import groovy.transform.builder.Builder

@EqualsAndHashCode(includes = 'name')
@Builder
class Field {

    String name

    Integer numberOfProfiles

    List<VkFaculty> faculties
}

package com.github.vuzoll.petrpigbyfield.domain

import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import groovy.transform.EqualsAndHashCode
import groovy.transform.builder.Builder
import org.springframework.data.annotation.Id

@EqualsAndHashCode(includes = 'name')
@Builder
class Field {

    @Id
    String id

    String name

    Integer numberOfProfiles

    List<VkFaculty> faculties
}

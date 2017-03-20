package com.github.vuzoll.petrpigbyfield.domain

import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import groovy.transform.EqualsAndHashCode
import groovy.transform.builder.Builder
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed

@EqualsAndHashCode(includes = 'name')
@Builder
class Field {

    @Id
    String id

    @Indexed
    String datasetName

    String name

    Integer numberOfProfiles

    List<VkFaculty> faculties
}

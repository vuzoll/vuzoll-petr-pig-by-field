package com.github.vuzoll.petrpigbyfield.domain.vk

import groovy.transform.EqualsAndHashCode
import groovy.transform.builder.Builder
import org.springframework.data.annotation.Id

@EqualsAndHashCode(includes = 'facultyId')
@Builder
class VkFaculty {

    @Id
    String id

    VkUniversity university
    Integer facultyId
    String facultyName

    Integer numberOfProfiles

    String field
}

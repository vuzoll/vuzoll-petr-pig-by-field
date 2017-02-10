package com.github.vuzoll.petrpigbyfield.domain.vk

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'vkId')
class VkCountry {

    Integer vkId
    String name
}

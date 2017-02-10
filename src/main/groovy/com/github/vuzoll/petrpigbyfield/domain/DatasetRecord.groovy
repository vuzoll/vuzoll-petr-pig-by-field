package com.github.vuzoll.petrpigbyfield.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.builder.Builder
import org.springframework.data.annotation.Id

@EqualsAndHashCode(includes = [ 'datasetName', 'rowNumber' ])
@Builder
class DatasetRecord {

    @Id
    String id

    String datasetName

    Integer rowNumber

    String row
}

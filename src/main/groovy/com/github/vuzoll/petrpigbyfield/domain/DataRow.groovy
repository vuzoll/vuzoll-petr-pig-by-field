package com.github.vuzoll.petrpigbyfield.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.builder.Builder
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed

@EqualsAndHashCode(includes = [ 'datasetName', 'rowNumber' ])
@Builder
class DataRow {

    @Id
    String id

    @Indexed
    String datasetName

    Integer rowNumber

    String row
}

package com.github.vuzoll.petrpigbyfield.repository

import com.github.vuzoll.petrpigbyfield.domain.Field
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource(collectionResourceRel = 'field', path = 'field')
interface FieldRepository extends PagingAndSortingRepository<Field, String> {

    Collection<Field> findByName(String name)

    void deleteByDatasetName(String datasetName)
}

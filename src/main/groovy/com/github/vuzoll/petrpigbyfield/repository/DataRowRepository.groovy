package com.github.vuzoll.petrpigbyfield.repository

import com.github.vuzoll.petrpigbyfield.domain.DataRow
import org.springframework.data.repository.PagingAndSortingRepository

interface DataRowRepository extends PagingAndSortingRepository<DataRow, String> {

    List<DataRow> findByDatasetNameOrderByRowNumber(String datasetName)
}

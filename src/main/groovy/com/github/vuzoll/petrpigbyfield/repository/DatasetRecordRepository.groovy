package com.github.vuzoll.petrpigbyfield.repository

import com.github.vuzoll.petrpigbyfield.domain.DatasetRecord
import org.springframework.data.repository.PagingAndSortingRepository

interface DatasetRecordRepository extends PagingAndSortingRepository<DatasetRecord, String> {

    List<DatasetRecord> findByDatasetNameOrderByRowNumber(String datasetName)
}

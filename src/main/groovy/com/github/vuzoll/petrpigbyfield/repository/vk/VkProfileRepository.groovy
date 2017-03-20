package com.github.vuzoll.petrpigbyfield.repository.vk

import com.github.vuzoll.petrpigbyfield.domain.vk.VkProfile
import org.springframework.data.repository.PagingAndSortingRepository

interface VkProfileRepository extends PagingAndSortingRepository<VkProfile, String> {

    long countByDatasetName(String datasetName)
}
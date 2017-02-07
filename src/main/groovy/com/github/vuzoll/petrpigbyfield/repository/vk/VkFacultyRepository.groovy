package com.github.vuzoll.petrpigbyfield.repository.vk

import com.github.vuzoll.petrpigbyfield.domain.vk.VkFaculty
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource(collectionResourceRel = 'faculty', path = 'faculty')
interface VkFacultyRepository extends PagingAndSortingRepository<VkFaculty, String> {

    VkFaculty findOneByFacultyId(String facultyId)
}

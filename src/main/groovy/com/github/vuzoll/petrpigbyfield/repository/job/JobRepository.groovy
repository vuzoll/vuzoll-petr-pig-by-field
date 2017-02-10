package com.github.vuzoll.petrpigbyfield.repository.job

import com.github.vuzoll.petrpigbyfield.domain.job.Job
import org.springframework.data.repository.PagingAndSortingRepository

interface JobRepository extends PagingAndSortingRepository<Job, String> {

    Collection<Job> findByStatus(String status)
}
package com.github.vuzoll.petrpigbyfield.domain.job

import org.springframework.data.annotation.Id

class Job {

    @Id
    String id

    String name

    Long startTimestamp
    String startTime

    String lastUpdateTime

    String endTime
    String timeTaken

    String status
    String message
}

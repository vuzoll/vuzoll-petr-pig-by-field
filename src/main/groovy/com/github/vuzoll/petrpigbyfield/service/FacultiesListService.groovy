package com.github.vuzoll.petrpigbyfield.service

class FacultiesListService {

    DurableJob prepareFacultiesListJob() {
        new DurableJob(name: 'prepare faculties list')
    }
}

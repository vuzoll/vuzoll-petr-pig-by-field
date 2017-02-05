package com.github.vuzoll.petrpigbyfield.service

abstract class DurableJob {

    final String name

    boolean finished

    DurableJob(String name) {
        this.name = name
        this.finished = false
    }

    abstract void doSomething(Closure statusUpdater)

    void markFinished() {
        this.finished = true
    }
}

package com.github.vuzoll.petrpigbyfield

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor

@SpringBootApplication
class Application {

    @Bean
    TaskExecutor taskExecutor() {
        new SimpleAsyncTaskExecutor()
    }

    static void main(String[] args) {
        SpringApplication.run(Application, args)
    }
}

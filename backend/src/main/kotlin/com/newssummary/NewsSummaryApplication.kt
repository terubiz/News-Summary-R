package com.newssummary

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class NewsSummaryApplication

fun main(args: Array<String>) {
    runApplication<NewsSummaryApplication>(*args)
}

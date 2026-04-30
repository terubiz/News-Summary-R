package com.newssummary

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NewsSummaryApplication

fun main(args: Array<String>) {
    runApplication<NewsSummaryApplication>(*args)
}

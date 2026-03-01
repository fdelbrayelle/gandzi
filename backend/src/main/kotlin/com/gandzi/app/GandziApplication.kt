package com.gandzi.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GandziApplication

fun main(args: Array<String>) {
    runApplication<GandziApplication>(*args)
}

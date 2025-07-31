package com.server.dpmcore

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class DpmCoreServerApplication

fun main(args: Array<String>) {
    runApplication<DpmCoreServerApplication>(*args)
}

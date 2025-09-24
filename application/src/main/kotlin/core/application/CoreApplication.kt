package core.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CoreApplication

fun main(args: Array<String>) {
    runApplication<CoreApplication>(*args)
}

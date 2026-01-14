package core.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = ["core.application", "core.persistence"])
class CoreApplication

fun main(args: Array<String>) {
    runApplication<CoreApplication>(*args)
}

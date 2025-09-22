package core.application.cohort.application.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cohort")
data class CohortProperties(
    var value: String = "17",
)

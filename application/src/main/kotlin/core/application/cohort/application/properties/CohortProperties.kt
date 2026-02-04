package core.application.cohort.application.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cohort")
data class CohortProperties(
    // TODO : 기수가 변경될 경우 수정 필요
    var value: String = "17",
)

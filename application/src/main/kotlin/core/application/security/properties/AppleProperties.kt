package core.application.security.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "apple")
data class AppleProperties(
    val teamId: String,
    val clientId: String,
    val keyId: String,
    val privateKey: String,
    val redirectUri: String,
)

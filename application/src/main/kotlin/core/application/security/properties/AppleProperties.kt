package core.application.security.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource

@ConfigurationProperties(prefix = "apple")
data class AppleProperties(
    val teamId: String,
    val clientId: String,
    val keyId: String, // id_token 의 apple RSA public Key
    val privateKeyPath: Resource,
    val redirectUri: String,
)

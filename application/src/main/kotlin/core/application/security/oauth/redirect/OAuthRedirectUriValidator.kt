package core.application.security.oauth.redirect

import core.application.security.properties.CorsProperties
import org.springframework.stereotype.Component
import java.net.URI

@Component
class OAuthRedirectUriValidator(
    private val corsProperties: CorsProperties,
) {
    fun validate(redirectUri: String): String {
        val uri = URI.create(redirectUri)
        val origin = uri.toOrigin()

        require(origin in corsProperties.allowedOrigins) {
            "Unsupported OAuth redirect origin: $origin"
        }

        return redirectUri
    }

    private fun URI.toOrigin(): String {
        require(!scheme.isNullOrBlank() && !host.isNullOrBlank()) {
            "OAuth redirect URI must be absolute"
        }

        val portSegment = if (port == -1) "" else ":$port"
        return "$scheme://$host$portSegment"
    }
}

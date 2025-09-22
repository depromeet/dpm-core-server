package core.application.security.properties

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.security.jwt")
data class TokenProperties(
    @NotNull val secretKey: String,
    @NotNull val expirationTime: ExpirationTime,
) {
    data class ExpirationTime(
        @Min(0) val accessToken: Long,
        @Min(0) val refreshToken: Long,
    )
}

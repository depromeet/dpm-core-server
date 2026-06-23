package core.application.refreshToken.presentation.response

import core.application.security.properties.TokenProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TokenResponseTest {
    @Test
    fun `of maps the token field`() {
        val tokenResponse =
            TokenResponse.of(
                token = "new-token",
                tokenProperties = createTokenProperties(),
            )

        assertThat(tokenResponse.token).isEqualTo("new-token")
        assertThat(tokenResponse.expirationTime).isEqualTo(3600L)
    }

    private fun createTokenProperties(): TokenProperties =
        TokenProperties(
            secretKey = "secret",
            expirationTime =
                TokenProperties.ExpirationTime(
                    accessToken = 3600L,
                    refreshToken = 1209600L,
                ),
        )
}

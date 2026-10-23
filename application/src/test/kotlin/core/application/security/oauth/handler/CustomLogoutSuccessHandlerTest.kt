package core.application.security.oauth.handler

import com.fasterxml.jackson.databind.ObjectMapper
import core.application.common.exception.CustomResponse
import core.application.refreshToken.application.service.RefreshTokenInvalidateService
import core.application.refreshToken.application.support.TokenHasher
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.oauth.token.JwtTokenResolver
import core.application.security.properties.SecurityProperties
import core.application.security.properties.TokenProperties
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.member.enums.LoginMethod
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant
import java.util.Base64

class CustomLogoutSuccessHandlerTest {
    private val properties =
        TokenProperties(
            secretKey = Base64.getEncoder().encodeToString("logout-test-signing-key-for-cookie-regression".toByteArray()),
            expirationTime = TokenProperties.ExpirationTime(accessToken = 7200L, refreshToken = 2592000L),
        )
    private val provider = JwtTokenProvider(properties, mock(RoleQueryUseCase::class.java))
    private val tokens = mock(RefreshTokenPersistencePort::class.java)
    private val objectMapper = ObjectMapper()
    private val handler =
        CustomLogoutSuccessHandler(
            JwtTokenInjector(
                properties,
                SecurityProperties("/logout", SecurityProperties.Cookie(".example.com", httpOnly = true, secure = true)),
            ),
            provider,
            JwtTokenResolver(),
            RefreshTokenInvalidateService(tokens),
            tokens,
            objectMapper,
        )

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `logout expires both cookies and deletes only the presented refresh session`(refreshInCookie: Boolean) {
        val refreshToken = provider.generateRefreshToken("1", LoginIdentity(LoginMethod.KAKAO, 10L))
        val hash = TokenHasher.sha256Hex(refreshToken)
        `when`(tokens.findByTokenHash(hash)).thenReturn(
            RefreshToken(
                memberId = MemberId(1L),
                tokenHash = hash,
                deviceId = "test-device",
                issuedAt = Instant.now(),
                expiresAt = Instant.now().plusSeconds(3600),
            ),
        )
        val request =
            MockHttpServletRequest().apply {
                if (refreshInCookie) {
                    addHeader("Authorization", "Bearer ${provider.generateAccessToken("1", LoginIdentity(LoginMethod.KAKAO, 10L))}")
                    setCookies(Cookie("refreshToken", refreshToken))
                } else {
                    addHeader("Authorization", "Bearer $refreshToken")
                }
            }

        logoutAndAssertResponse(request)

        verify(tokens).deleteByTokenHash(hash)
        verify(tokens, never()).deleteByMemberId(anyLong())
    }

    @Test
    fun `logout retains all device fallback when only an access bearer is available`() {
        val request =
            MockHttpServletRequest().apply {
                addHeader("Authorization", "Bearer ${provider.generateAccessToken("1", LoginIdentity(LoginMethod.KAKAO, 10L))}")
            }

        logoutAndAssertResponse(request)

        verify(tokens).deleteByMemberId(1L)
        verify(tokens, never()).deleteByTokenHash(anyString())
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `logout expires both cookies without deleting sessions for absent or invalid tokens`(invalidTokens: Boolean) {
        val request =
            MockHttpServletRequest().apply {
                if (invalidTokens) {
                    addHeader("Authorization", "Bearer invalid-token")
                    setCookies(Cookie("accessToken", "invalid-access"), Cookie("refreshToken", "invalid-refresh"))
                }
            }

        logoutAndAssertResponse(request)

        verify(tokens, never()).deleteByTokenHash(anyString())
        verify(tokens, never()).deleteByMemberId(anyLong())
    }

    private fun logoutAndAssertResponse(request: MockHttpServletRequest) {
        val authentication = UsernamePasswordAuthenticationToken("1", null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication
        val response = MockHttpServletResponse()

        handler.onLogoutSuccess(request, response, authentication)

        assertThat(response.status).isEqualTo(200)
        assertThat(response.contentType).isEqualTo("application/json;charset=UTF-8")
        assertThat(objectMapper.readTree(response.contentAsString))
            .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(CustomResponse.ok<Unit>())))
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
        assertThat(response.cookies.map { it.name }).containsExactlyInAnyOrder("accessToken", "refreshToken")
        response.cookies.forEach {
            assertThat(it.value).isEmpty()
            assertThat(it.maxAge).isZero()
            assertThat(it.domain).isEqualTo("example.com")
            assertThat(it.path).isEqualTo("/")
            assertThat(it.secure).isTrue()
            assertThat(it.isHttpOnly).isTrue()
            assertThat(it.getAttribute("SameSite")).isEqualTo(if (it.name == "accessToken") "Lax" else "None")
        }
    }
}

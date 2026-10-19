package core.application.security.oauth.handler

import core.application.member.application.exception.MemberDeletedException
import core.application.security.oauth.domain.CustomOAuth2User
import core.application.security.oauth.exception.OAuthExceptionCode
import core.application.security.oauth.redirect.OAuthCallbackRedirectService
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.domain.member.port.inbound.HandleMemberLoginUseCase
import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.security.oauth.dto.LoginResult
import core.domain.security.oauth.dto.OAuthAttributes
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant

class CustomAuthenticationSuccessHandlerTest {
    private val injector = mock(JwtTokenInjector::class.java)
    private val login = mock(HandleMemberLoginUseCase::class.java)
    private val redirects = mock(OAuthCallbackRedirectService::class.java)
    private val devices = mock(DeviceIdResolver::class.java)
    private val handler = CustomAuthenticationSuccessHandler(injector, login, redirects, devices)
    private val request = MockHttpServletRequest()
    private val response = MockHttpServletResponse()
    private val attributes = mock(OAuthAttributes::class.java)
    private val principal = CustomOAuth2User(emptyList(), mapOf("id" to "test-id"), "id", attributes)
    private val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())

    init {
        `when`(devices.resolve(request, response)).thenReturn("device")
    }

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    @Test
    fun `deleted member callback returns failure without issuing token and clears authentication`() {
        `when`(login.handleLoginSuccess(attributes, "device")).thenThrow(MemberDeletedException())
        `when`(redirects.buildFailureRedirectUri(request, "MEMBER-401-02"))
            .thenReturn("https://client.example.com/?authenticated=false&errorCode=MEMBER-401-02")
        SecurityContextHolder.getContext().authentication = authentication
        request.getSession(true)

        handler.onAuthenticationSuccess(request, response, authentication)

        assertThat(response.status).isEqualTo(302)
        assertThat(response.redirectedUrl).contains("authenticated=false", "errorCode=MEMBER-401-02")
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
        assertThat(request.getSession(false)).isNull()
        verify(injector).invalidateRefreshToken(response)
        verifyNoMoreInteractions(injector)
        verify(redirects).clearClientRedirectUri(response)
    }

    @Test
    fun `login without token cannot redirect as authenticated`() {
        `when`(login.handleLoginSuccess(attributes, "device")).thenReturn(LoginResult(null))
        `when`(redirects.buildFailureRedirectUri(request, OAuthExceptionCode.AUTHENTICATION_FAILED.getCode()))
            .thenReturn("https://client.example.com/?authenticated=false")

        handler.onAuthenticationSuccess(request, response, authentication)

        assertThat(response.redirectedUrl).contains("authenticated=false")
        verify(injector).invalidateRefreshToken(response)
        verifyNoMoreInteractions(injector)
    }

    @Test
    fun `successful login still injects token and redirects`() {
        val refresh = RefreshToken.issue(MemberId(1), "test-token", "hash", "device", Instant.EPOCH, Instant.MAX)
        `when`(login.handleLoginSuccess(attributes, "device")).thenReturn(LoginResult(refresh))
        `when`(redirects.buildSuccessRedirectUri(request)).thenReturn("https://client.example.com/?authenticated=true")

        handler.onAuthenticationSuccess(request, response, authentication)

        assertThat(response.redirectedUrl).contains("authenticated=true")
        verify(injector).injectRefreshToken(refresh, response)
        verifyNoMoreInteractions(injector)
        verify(redirects).clearClientRedirectUri(response)
    }

    @Test
    fun `unexpected failures are not reported as deleted members`() {
        `when`(login.handleLoginSuccess(attributes, "device")).thenThrow(IllegalStateException("failure"))

        assertThatThrownBy { handler.onAuthenticationSuccess(request, response, authentication) }
            .isInstanceOf(IllegalStateException::class.java)
        verifyNoMoreInteractions(injector, redirects)
    }
}

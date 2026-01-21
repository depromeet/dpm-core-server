package core.application.member.application.service.auth

import core.application.member.application.service.oauth.MemberOAuthService
import core.application.security.oauth.apple.AppleTokenExchangeService
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import core.application.member.application.exception.MemberNotFoundException

import org.mockito.Mockito
import org.springframework.core.env.Environment

class AppleAuthServiceTest {

    private val appleTokenExchangeService: AppleTokenExchangeService = mock(AppleTokenExchangeService::class.java)
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort = mock(MemberOAuthPersistencePort::class.java)
    private val memberPersistencePort: MemberPersistencePort = mock(MemberPersistencePort::class.java)
    private val memberOAuthService: MemberOAuthService = mock(MemberOAuthService::class.java)
    private val jwtTokenProvider: JwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort = mock(RefreshTokenPersistencePort::class.java)
    private val environment: Environment = mock(Environment::class.java)

    private val appleIdTokenValidator: core.application.security.oauth.apple.AppleIdTokenValidator = mock(core.application.security.oauth.apple.AppleIdTokenValidator::class.java)

    private val appleAuthService = AppleAuthService(
        appleTokenExchangeService,
        memberOAuthPersistencePort,
        memberPersistencePort,
        memberOAuthService,
        jwtTokenProvider,
        refreshTokenPersistencePort,
        environment,
        appleIdTokenValidator
    )

    @Test
    fun `login should throw MemberNotFoundException if member not exists`() {
        // Arrange
        val authCode = "valid-code"
        val idToken = "id-token"
        val tokenResponse = AppleTokenExchangeService.AppleTokenResponse(
            access_token = "access",
            token_type = "Bearer",
            expires_in = 3600,
            refresh_token = "refresh",
            id_token = idToken
        )

        `when`(appleTokenExchangeService.getTokens(authCode)).thenReturn(tokenResponse)

        val claims = Mockito.mock(io.jsonwebtoken.Claims::class.java)
        `when`(claims.subject).thenReturn("user-123")
        `when`(appleIdTokenValidator.verify(idToken)).thenReturn(claims)

        `when`(memberOAuthPersistencePort.findByProviderAndExternalId(OAuthProvider.APPLE, "user-123")).thenReturn(null)

        // Act & Assert
        assertThrows(MemberNotFoundException::class.java) {
            appleAuthService.login(authCode)
        }

        verify(memberPersistencePort, times(0)).save(anyObject())
    }

    @Test
    fun `login should login existing member`() {
        // Arrange
        val authCode = "valid-code"
        val idToken = "id-token"
         val tokenResponse = AppleTokenExchangeService.AppleTokenResponse(
            access_token = "access",
            token_type = "Bearer",
            expires_in = 3600,
            refresh_token = "refresh",
            id_token = idToken
        )

        `when`(appleTokenExchangeService.getTokens(authCode)).thenReturn(tokenResponse)

        val claims = Mockito.mock(io.jsonwebtoken.Claims::class.java)
        `when`(claims.subject).thenReturn("user-123")
        `when`(appleIdTokenValidator.verify(idToken)).thenReturn(claims) // No email needed

        val existingMemberId = MemberId(1L)
        val memberOAuth = MemberOAuth(
            id = MemberOAuthId(10L),
            externalId = "user-123",
            provider = OAuthProvider.APPLE,
            memberId = existingMemberId
        )
        `when`(memberOAuthPersistencePort.findByProviderAndExternalId(OAuthProvider.APPLE, "user-123")).thenReturn(memberOAuth)

        val existingMember = Member(
            id = existingMemberId,
            email = "test@apple.com",
            signupEmail = "test@apple.com",
            name = "Apple User",
            status = core.domain.member.enums.MemberStatus.ACTIVE,
            part = core.domain.member.enums.MemberPart.SERVER
        )
        `when`(memberPersistencePort.findById(1L)).thenReturn(existingMember)

        `when`(jwtTokenProvider.generateAccessToken("1")).thenReturn("app-access")
        `when`(jwtTokenProvider.generateRefreshToken("1")).thenReturn("app-refresh")

        // Act
        val result = appleAuthService.login(authCode)

        // Assert
        verify(memberPersistencePort, times(0)).save(anyObject())
        verify(memberOAuthService, times(0)).addMemberOAuthProvider(anyObject(), anyObject())
        assertEquals("app-access", result.accessToken)
    }

    private fun <T> anyObject(): T {
        Mockito.any<T>()
        return uninitialized()
    }
    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}

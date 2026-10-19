package core.application.member.application.service

import core.application.authorization.application.service.RoleQueryService
import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.service.auth.AppleAuthService
import core.application.member.application.service.auth.EmailPasswordAuthService
import core.application.member.application.service.auth.KakaoAuthService
import core.application.member.application.service.auth.KakaoLoginTokenSaveService
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.refreshToken.application.service.RefreshTokenIssueService
import core.application.security.oauth.apple.AppleIdTokenValidator
import core.application.security.oauth.apple.AppleTokenExchangeService
import core.application.security.oauth.kakao.KakaoUserInfoClient
import core.application.security.oauth.redirect.OAuthRedirectUriValidator
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.MemberStatus
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.domain.membercredential.aggregate.MemberCredential
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.domain.security.oauth.dto.KakaoAuthAttributes
import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant

class DeletedMemberLoginTest {
    private val identityLock = mock(MemberIdentityLockService::class.java)
    private val members = mock(MemberPersistencePort::class.java)
    private val oauth = mock(MemberOAuthPersistencePort::class.java)
    private val issuer = mock(RefreshTokenIssueService::class.java)
    private val tokens = mock(JwtTokenProvider::class.java)
    private val roles = mock(MemberRoleService::class.java)
    private val teams = mock(MemberTeamService::class.java)
    private val redirects = mock(OAuthRedirectUriValidator::class.java)
    private val deleted = Member(MemberId(1), "홍길동", signupEmail = EMAIL, status = MemberStatus.WITHDRAWN, deletedAt = Instant.now())

    @Test
    fun `Kakao 로그인은 삭제 회원의 기존 연결을 갱신하거나 새 회원으로 복원하지 않는다`() {
        val client = mock(KakaoUserInfoClient::class.java)
        `when`(client.getUserAttributes("code", null)).thenReturn(mapOf("id" to 101L, "kakao_account" to mapOf("email" to EMAIL, "profile" to mapOf("nickname" to "김철수"))))
        val service = KakaoAuthService(client, redirects, oauth, members, identityLock, tokens, issuer, roles, teams)
        listOf(true, false).forEach { linked ->
            prepare(OAuthProvider.KAKAO, linked)
            assertThatThrownBy { service.login("code") }.isInstanceOf(MemberDeletedException::class.java)
            assertReadOnly(OAuthProvider.KAKAO, linked)
        }
    }

    @Test
    fun `Apple 로그인은 삭제 회원의 기존 연결을 갱신하거나 새 회원으로 복원하지 않는다`() {
        val exchange = mock(AppleTokenExchangeService::class.java)
        val validator = mock(AppleIdTokenValidator::class.java)
        `when`(exchange.getTokens("code", null)).thenReturn(AppleTokenExchangeService.AppleTokenResponse("access", "Bearer", 3600, null, "id"))
        `when`(validator.verify("id")).thenReturn(Jwts.claims().subject("101").add("email", EMAIL).build())
        val service = AppleAuthService(exchange, redirects, oauth, members, identityLock, tokens, issuer, validator, roles, teams)
        listOf(true, false).forEach { linked ->
            prepare(OAuthProvider.APPLE, linked)
            assertThatThrownBy { service.login("code") }.isInstanceOf(MemberDeletedException::class.java)
            assertReadOnly(OAuthProvider.APPLE, linked)
        }
    }

    @Test
    fun `웹 OAuth와 Native 공통 로그인은 삭제된 연결의 이메일조차 변경하지 않는다`() {
        prepare(OAuthProvider.KAKAO, true)
        val service = MemberLoginService(members, identityLock, MemberOAuthService(oauth), issuer, roles, teams)
        assertThat(service.handleLoginSuccess(KakaoAuthAttributes("101", EMAIL, "김철수", OAuthProvider.KAKAO), null).refreshToken).isNull()
        assertReadOnly(OAuthProvider.KAKAO, true)
    }

    @Test
    fun `비밀번호 로그인은 삭제된 자격증명을 보존하며 신규가입도 차단한다`() {
        val credentials = mock(MemberCredentialPersistencePort::class.java)
        val encoder = mock(PasswordEncoder::class.java)
        val roleQueries = mock(RoleQueryService::class.java)
        val service = EmailPasswordAuthService(credentials, members, identityLock, roleQueries, roles, teams, tokens, issuer, encoder)
        `when`(credentials.findByEmail(EMAIL)).thenReturn(MemberCredential(memberId = MemberId(1), email = EMAIL, password = "hash"))
        `when`(encoder.matches("password", "hash")).thenReturn(true)
        `when`(members.findById(MemberId(1))).thenReturn(deleted)
        assertThatThrownBy { service.login(EMAIL, "password") }.isInstanceOf(BusinessException::class.java)
        verify(credentials).findByEmail(EMAIL)
        verifyNoMoreInteractions(credentials)
        reset(credentials, members)
        `when`(members.findAllBySignupEmail(EMAIL)).thenReturn(listOf(deleted))
        assertThatThrownBy { service.login(EMAIL, "password") }.isInstanceOf(BusinessException::class.java)
        verify(credentials).findByEmail(EMAIL)
        verify(members).findAllBySignupEmail(EMAIL)
        verifyNoMoreInteractions(credentials, members)
        verifyNoInteractions(tokens, issuer, roles, teams, roleQueries)
    }

    @Test
    fun `body refresh 토큰 쿠키 저장 경로도 삭제 회원에게 새 세션을 발급하지 않는다`() {
        val injector = mock(JwtTokenInjector::class.java)
        val device = mock(DeviceIdResolver::class.java)
        val service = KakaoLoginTokenSaveService(tokens, injector, issuer, device, members)
        `when`(tokens.validateToken("refresh")).thenReturn(true)
        `when`(tokens.getMemberId("refresh")).thenReturn(1L)
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.WITHDRAWN, emptySet(), true)))
        assertThatThrownBy { service.save("refresh", MockHttpServletRequest(), MockHttpServletResponse()) }.isInstanceOf(TokenInvalidException::class.java)
        verifyNoInteractions(injector, issuer, device)
    }

    private fun prepare(
        provider: OAuthProvider,
        linked: Boolean,
    ) {
        reset(oauth, members)
        `when`(members.findById(MemberId(1))).thenReturn(deleted)
        `when`(members.findAllBySignupEmail(EMAIL)).thenReturn(listOf(deleted))
        if (linked) `when`(oauth.findByProviderAndExternalId(provider, "101")).thenReturn(MemberOAuth(MemberOAuthId(1), "101", provider, MemberId(1), EMAIL))
    }

    private fun assertReadOnly(
        provider: OAuthProvider,
        linked: Boolean,
    ) {
        verify(oauth).findByProviderAndExternalId(provider, "101")
        if (linked) verify(members).findById(MemberId(1)) else verify(members).findAllBySignupEmail(EMAIL)
        verifyNoMoreInteractions(oauth, members)
        verifyNoInteractions(tokens, issuer, roles, teams)
    }

    private companion object {
        const val EMAIL = "hong@example.com"
    }
}

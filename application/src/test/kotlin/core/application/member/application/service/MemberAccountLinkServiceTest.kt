package core.application.member.application.service

import core.application.member.application.exception.MemberAccountException
import core.application.security.oauth.apple.AppleIdTokenValidator
import core.application.security.oauth.apple.AppleTokenExchangeService
import core.application.security.oauth.kakao.KakaoTokenExchangeService
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.OAuthLinkRequest
import core.domain.member.vo.LoginIdentity
import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

class MemberAccountLinkServiceTest {
    private val requests = mock(MemberAccountLinkRequestService::class.java)
    private val accounts = mock(MemberAccountService::class.java)
    private val apple = mock(AppleTokenExchangeService::class.java)
    private val validator = mock(AppleIdTokenValidator::class.java)
    private val kakao = mock(KakaoTokenExchangeService::class.java)
    private val identity = LoginIdentity(LoginMethod.KAKAO, 10)
    private val service = MemberAccountLinkService(requests, accounts, apple, validator, kakao)

    @Test
    fun `Apple 인증 코드가 다른 연결 요청에서 발급되었으면 거절한다`() {
        `when`(requests.consume(1, OAuthProvider.APPLE, "state")).thenReturn(request(OAuthProvider.APPLE))
        `when`(apple.getTokens("code", REDIRECT)).thenReturn(AppleTokenExchangeService.AppleTokenResponse("access", "Bearer", 100, null, "id-token"))
        `when`(validator.verify("id-token")).thenReturn(Jwts.claims().subject("apple-subject").add("nonce", "other-request").build())
        assertThatThrownBy { service.complete(1, identity, OAuthProvider.APPLE, "state", "code") }.isInstanceOf(MemberAccountException::class.java)
        verifyNoInteractions(accounts)
    }

    @Test
    fun `만료되거나 소비된 state는 제공자 토큰 교환도 하지 않는다`() {
        `when`(requests.consume(1, OAuthProvider.APPLE, "state")).thenThrow(MemberAccountException(core.application.member.application.exception.MemberExceptionCode.MEMBER_ACCOUNT_LINK_INVALID))
        assertThatThrownBy { service.complete(1, identity, OAuthProvider.APPLE, "state", "code") }.isInstanceOf(MemberAccountException::class.java)
        verifyNoInteractions(apple, validator, kakao, accounts)
    }

    @Test
    fun `Kakao 코드는 요청에 저장된 redirect와 PKCE verifier로 교환한다`() {
        `when`(requests.consume(1, OAuthProvider.KAKAO, "state")).thenReturn(request(OAuthProvider.KAKAO))
        `when`(kakao.getLinkUserAttributes("code", REDIRECT, "verifier")).thenThrow(IllegalArgumentException("provider secret must not leak"))
        assertThatThrownBy { service.complete(1, identity, OAuthProvider.KAKAO, "state", "code") }.isInstanceOf(MemberAccountException::class.java).hasMessageNotContaining("provider secret")
        verify(kakao).getLinkUserAttributes("code", REDIRECT, "verifier")
        verifyNoInteractions(accounts)
    }

    @Test
    fun `Apple 검증된 nonce와 sub만 현재 회원에게 연결한다`() {
        `when`(requests.consume(1, OAuthProvider.APPLE, "state")).thenReturn(request(OAuthProvider.APPLE))
        `when`(apple.getTokens("code", REDIRECT)).thenReturn(AppleTokenExchangeService.AppleTokenResponse("access", "Bearer", 100, null, "id-token"))
        `when`(validator.verify("id-token")).thenReturn(Jwts.claims().subject("apple-subject").add("nonce", "nonce").add("email", "relay@example.com").build())
        var linked = false
        val destination =
            mock(
                MemberAccountService::class.java,
                org.mockito.stubbing.Answer { invocation ->
                    if (invocation.method.name == "link") {
                        val proof = invocation.getArgument<core.domain.security.oauth.dto.OAuthAttributes>(2)
                        org.assertj.core.api.Assertions.assertThat(proof.getExternalId()).isEqualTo("apple-subject")
                        org.assertj.core.api.Assertions.assertThat(proof.getProvider()).isEqualTo(OAuthProvider.APPLE)
                        linked = true
                        core.application.member.presentation.response.MemberAccountResponse(emptyList())
                    } else {
                        org.mockito.Mockito.RETURNS_DEFAULTS.answer(invocation)
                    }
                },
            )
        MemberAccountLinkService(requests, destination, apple, validator, kakao).complete(1, identity, OAuthProvider.APPLE, "state", "code")
        org.assertj.core.api.Assertions.assertThat(linked).isTrue()
    }

    private fun request(provider: OAuthProvider) = OAuthLinkRequest("nonce", 1, provider, REDIRECT, "verifier", Instant.now().plusSeconds(300))

    private companion object {
        const val REDIRECT = "https://example.com/callback"
    }
}

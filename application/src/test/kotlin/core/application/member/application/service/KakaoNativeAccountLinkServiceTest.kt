package core.application.member.application.service

import core.application.member.application.exception.MemberAccountException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberOAuthConflictException
import core.application.member.presentation.response.MemberAccountResponse
import core.application.security.oauth.kakao.KakaoAccessTokenInfo
import core.application.security.oauth.kakao.KakaoUserInfoClient
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.OAuthProvider
import core.domain.member.vo.LoginIdentity
import core.domain.security.oauth.dto.KakaoAuthAttributes
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class KakaoNativeAccountLinkServiceTest {
    private val requests = mock(MemberAccountLinkRequestService::class.java)
    private val accounts = mock(MemberAccountService::class.java)
    private val kakao = mock(KakaoUserInfoClient::class.java)
    private val identity = LoginIdentity(LoginMethod.APPLE, 11)
    private val service = KakaoNativeAccountLinkService(requests, accounts, kakao, 123)
    private val proof = KakaoAuthAttributes("456", "", "", OAuthProvider.KAKAO)

    @Test
    fun `현재 Apple 회원에 검증된 Kakao 계정을 연결하며 이메일이 없어도 허용한다`() {
        validToken()
        val expected = MemberAccountResponse(emptyList())
        `when`(accounts.link(1, identity, proof)).thenReturn(expected)
        assertThat(service.complete(1, identity, "state", "provider-token")).isSameAs(expected)
        val order = inOrder(requests, kakao, accounts)
        order.verify(requests).consumeNativeKakao(1, "state")
        order.verify(kakao).getAccessTokenInfo("provider-token")
        order.verify(kakao).getUserAttributesByAccessToken("provider-token")
        order.verify(accounts).link(1, identity, proof)
    }

    @Test
    fun `만료되거나 사용한 연결 요청은 제공자 검증 전에 거절한다`() {
        `when`(requests.consumeNativeKakao(1, "state")).thenThrow(MemberAccountException(MemberExceptionCode.MEMBER_ACCOUNT_LINK_INVALID))
        assertThatThrownBy { service.complete(1, identity, "state", "provider-token") }.isInstanceOf(MemberAccountException::class.java)
        verifyNoInteractions(kakao, accounts)
    }

    @Test
    fun `다른 앱의 토큰은 계정을 연결하지 않는다`() {
        `when`(kakao.getAccessTokenInfo("provider-token")).thenReturn(KakaoAccessTokenInfo(456, 999, 300))
        assertThatThrownBy { service.complete(1, identity, "state", "provider-token") }.isInstanceOf(MemberAccountException::class.java)
        verifyNoInteractions(accounts)
    }

    @Test
    fun `만료 토큰은 계정을 연결하지 않는다`() {
        `when`(kakao.getAccessTokenInfo("provider-token")).thenReturn(KakaoAccessTokenInfo(456, 123, 0))
        assertThatThrownBy { service.complete(1, identity, "state", "provider-token") }.isInstanceOf(MemberAccountException::class.java)
        verifyNoInteractions(accounts)
    }

    @Test
    fun `앱 ID 설정이 없으면 실패하며 제공자 호출도 하지 않는다`() {
        assertThatThrownBy { KakaoNativeAccountLinkService(requests, accounts, kakao, 0).complete(1, identity, "state", "provider-token") }
            .isInstanceOf(MemberAccountException::class.java)
        verifyNoInteractions(kakao, accounts)
    }

    @Test
    fun `토큰 정보와 사용자 정보 ID가 다르면 거절한다`() {
        validToken()
        `when`(kakao.getUserAttributesByAccessToken("provider-token")).thenReturn(mapOf("id" to 789L))
        assertThatThrownBy { service.complete(1, identity, "state", "provider-token") }.isInstanceOf(MemberAccountException::class.java)
        verifyNoInteractions(accounts)
    }

    @Test
    fun `제공자 오류의 본문이나 토큰을 노출하지 않는다`() {
        `when`(kakao.getAccessTokenInfo("provider-token")).thenThrow(IllegalArgumentException("provider-token private-body"))
        assertThatThrownBy { service.complete(1, identity, "state", "provider-token") }
            .isInstanceOf(MemberAccountException::class.java).hasMessageNotContaining("provider-token").hasMessageNotContaining("private-body")
        verifyNoInteractions(accounts)
    }

    @Test
    fun `이미 다른 회원에 연결된 계정 충돌은 그대로 반환한다`() {
        validToken()
        `when`(accounts.link(1, identity, proof)).thenThrow(MemberOAuthConflictException())
        assertThatThrownBy { service.complete(1, identity, "state", "provider-token") }.isInstanceOf(MemberOAuthConflictException::class.java)
    }

    @Test
    fun `인증 중 해제된 서비스 로그인 수단은 연결 단계에서 다시 검증한다`() {
        validToken()
        `when`(accounts.link(1, identity, proof)).thenThrow(MemberAccountException(MemberExceptionCode.MEMBER_ACCOUNT_REAUTH_REQUIRED))
        assertThatThrownBy { service.complete(1, identity, "state", "provider-token") }.isInstanceOf(MemberAccountException::class.java)
    }

    private fun validToken() {
        `when`(kakao.getAccessTokenInfo("provider-token")).thenReturn(KakaoAccessTokenInfo(456, 123, 300))
        `when`(kakao.getUserAttributesByAccessToken("provider-token")).thenReturn(mapOf("id" to 456L))
    }
}

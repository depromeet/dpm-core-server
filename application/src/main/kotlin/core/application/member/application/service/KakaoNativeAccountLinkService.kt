package core.application.member.application.service

import core.application.member.application.exception.MemberAccountException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.presentation.response.MemberAccountResponse
import core.application.security.oauth.kakao.KakaoUserInfoClient
import core.domain.member.enums.OAuthProvider
import core.domain.member.vo.LoginIdentity
import core.domain.security.oauth.dto.KakaoAuthAttributes
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class KakaoNativeAccountLinkService(
    private val requests: MemberAccountLinkRequestService,
    private val accounts: MemberAccountService,
    private val kakao: KakaoUserInfoClient,
    @Value("\${KAKAO_APP_ID:0}") private val expectedAppId: Long,
) {
    fun complete(
        memberId: Long,
        identity: LoginIdentity?,
        state: String,
        accessToken: String,
    ): MemberAccountResponse {
        // Consume independently: failed provider verification cannot reuse this ceremony.
        requests.consumeNativeKakao(memberId, state)
        val proof =
            try {
                require(expectedAppId > 0)
                val token = kakao.getAccessTokenInfo(accessToken)
                require(token.appId == expectedAppId && token.expiresIn > 0 && token.id > 0)
                val user = kakao.getUserAttributesByAccessToken(accessToken)
                require(user["id"]?.toString()?.toLongOrNull() == token.id)
                val account = user["kakao_account"] as? Map<*, *>
                KakaoAuthAttributes(
                    token.id.toString(),
                    account?.get("email") as? String ?: "",
                    "",
                    OAuthProvider.KAKAO,
                )
            } catch (_: Exception) {
                // Provider bodies and tokens must not reach responses or logs.
                throw MemberAccountException(MemberExceptionCode.MEMBER_ACCOUNT_LINK_INVALID)
            }
        return accounts.link(memberId, identity, proof)
    }
}

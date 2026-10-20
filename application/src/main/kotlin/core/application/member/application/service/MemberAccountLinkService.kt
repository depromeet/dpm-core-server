package core.application.member.application.service

import core.application.member.application.exception.MemberAccountException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.presentation.response.MemberAccountResponse
import core.application.security.oauth.apple.AppleIdTokenValidator
import core.application.security.oauth.apple.AppleTokenExchangeService
import core.application.security.oauth.kakao.KakaoTokenExchangeService
import core.domain.member.enums.OAuthProvider
import core.domain.member.vo.LoginIdentity
import core.domain.security.oauth.dto.AppleAuthAttributes
import core.domain.security.oauth.dto.OAuthAttributes
import org.springframework.stereotype.Service

@Service
class MemberAccountLinkService(
    private val requests: MemberAccountLinkRequestService,
    private val accounts: MemberAccountService,
    private val apple: AppleTokenExchangeService,
    private val appleValidator: AppleIdTokenValidator,
    private val kakao: KakaoTokenExchangeService,
) {
    fun complete(
        memberId: Long,
        identity: LoginIdentity?,
        provider: OAuthProvider,
        state: String,
        authorizationCode: String,
    ): MemberAccountResponse {
        val request = requests.consume(memberId, provider, state)
        val proof =
            try {
                when (provider) {
                    OAuthProvider.APPLE -> {
                        val tokens = apple.getTokens(authorizationCode, request.redirectUri)
                        val claims = appleValidator.verify(tokens.id_token)
                        if (claims["nonce"] != request.stateHash || claims.subject.isNullOrBlank()) {
                            throw IllegalArgumentException()
                        }
                        AppleAuthAttributes.of(claims)
                    }
                    OAuthProvider.KAKAO ->
                        OAuthAttributes.of(
                            "KAKAO",
                            kakao.getLinkUserAttributes(authorizationCode, request.redirectUri, request.codeVerifier),
                        ) ?: throw IllegalArgumentException()
                }
            } catch (_: Exception) {
                // Do not expose provider responses containing authorization codes or tokens.
                throw MemberAccountException(MemberExceptionCode.MEMBER_ACCOUNT_LINK_INVALID)
            }
        return accounts.link(memberId, identity, proof)
    }
}

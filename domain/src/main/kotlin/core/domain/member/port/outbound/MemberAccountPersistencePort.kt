package core.domain.member.port.outbound

import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import java.time.Instant

interface MemberAccountPersistencePort {
    fun findOAuths(memberId: Long): List<MemberOAuth>

    fun deleteOAuth(
        memberId: Long,
        oauthId: Long,
    ): Int

    fun saveLinkRequest(request: OAuthLinkRequest)

    fun lockLinkRequest(stateHash: String): OAuthLinkRequest?

    fun deleteLinkRequest(stateHash: String)

    fun deleteExpiredLinkRequests(now: Instant)
}

data class OAuthLinkRequest(
    val stateHash: String,
    val memberId: Long,
    val provider: OAuthProvider,
    val redirectUri: String,
    val codeVerifier: String,
    val expiresAt: Instant,
)

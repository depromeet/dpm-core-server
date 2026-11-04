package core.application.security.oauth.token

import core.application.refreshToken.application.support.TokenHasher
import core.domain.member.port.outbound.MemberMergePersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import org.springframework.stereotype.Component

/** Legacy JWT subjects are accepted only through a recorded merge and the original login identity. */
@Component
class MergedMemberResolver(
    private val tokens: JwtTokenProvider,
    private val members: MemberPersistencePort,
    private val merges: MemberMergePersistencePort,
    private val identities: LoginIdentityValidator,
    private val refreshTokens: core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort,
) {
    data class ResolvedMember(val memberId: Long, val path: List<Long>)

    fun resolve(token: String): ResolvedMember? = resolve(token, true)

    /** Refresh discovers lock targets without caching OAuth entities before acquiring the member locks. */
    fun resolvePath(token: String): ResolvedMember? = resolve(token, false)

    private fun resolve(
        token: String,
        validateIdentity: Boolean,
    ): ResolvedMember? {
        if (!tokens.validateToken(token)) return null
        val identity = tokens.getLoginIdentity(token) ?: return null
        var memberId = tokens.getMemberId(token)
        val path = mutableListOf<Long>()
        repeat(9) {
            if (memberId in path) return null
            path.add(memberId)
            if (members.isLoginAvailable(memberId)) {
                if (validateIdentity && !identities.isValid(memberId, identity)) return null
                if (validateIdentity && path.size > 1 && tokens.isRefreshToken(token)) {
                    val stored = refreshTokens.findByTokenHash(TokenHasher.sha256Hex(token)) ?: return null
                    val now = java.time.Instant.now()
                    if (stored.memberId.value != memberId || stored.isExpired(now) ||
                        (stored.isRotated() && !stored.isWithinGrace(now, 60))
                    ) {
                        return null
                    }
                }
                return ResolvedMember(memberId, path)
            }
            val merge = merges.findTarget(memberId) ?: return null
            val issuedAt = tokens.getIssuedAt(token) ?: return null
            // JWT iat is encoded in seconds, whereas the merge event has subsecond precision.
            if (issuedAt.epochSecond > merge.mergedAt.epochSecond) return null
            memberId = merge.retainedMemberId
        }
        return null
    }
}

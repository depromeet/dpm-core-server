package core.application.security.oauth.token

import core.domain.member.enums.LoginMethod
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.domain.refreshToken.aggregate.RefreshToken.Companion.GRACE_SECONDS
import core.domain.refreshToken.aggregate.RefreshToken.Companion.MAX_SUCCESSOR_DEPTH
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LoginIdentityValidator(
    private val oauth: MemberOAuthPersistencePort,
    private val credentials: MemberCredentialPersistencePort,
    private val refreshTokens: RefreshTokenPersistencePort,
) {
    fun isValid(
        memberId: Long,
        identity: LoginIdentity?,
    ): Boolean {
        if (identity == null) return false
        if (identity.method == LoginMethod.EMAIL) {
            return credentials.findByMemberId(MemberId(memberId))?.let {
                it.id?.value == identity.accountId && it.password.isNotBlank()
            } == true
        }
        return oauth.findById(MemberOAuthId(identity.accountId))?.let {
            it.memberId.value == memberId && LoginMethod.from(it.provider) == identity.method
        } == true || identity.sessionTokenHash?.let { hasContinuation(memberId, it) } == true
    }

    /** 회원 잠금 아래서 현재 세션만 허용한다. 오래된 토큰이면 해제 전에 재발급을 요청한다. */
    fun preserveCurrentSession(
        memberId: Long,
        identity: LoginIdentity,
    ): Boolean {
        val hash = identity.sessionTokenHash ?: return false
        val row = refreshTokens.lockByTokenHash(hash) ?: return false
        if (row.memberId.value != memberId || row.isExpired(Instant.now()) || row.isRotated() ||
            row.successorTokenHash != null
        ) {
            return false
        }
        refreshTokens.grantContinuation(hash, memberId)
        return true
    }

    private fun hasContinuation(
        memberId: Long,
        tokenHash: String,
    ): Boolean {
        val now = Instant.now()
        var hash = tokenHash
        repeat(MAX_SUCCESSOR_DEPTH) {
            val row = refreshTokens.findByTokenHash(hash) ?: return false
            if (row.memberId.value != memberId || row.isExpired(now)) {
                return false
            }
            if (!row.isRotated()) return row.successorTokenHash == null && row.continuationMemberId == memberId
            if (!row.isWithinGrace(now, GRACE_SECONDS)) return false
            hash = row.successorTokenHash ?: return false
        }
        return false
    }
}

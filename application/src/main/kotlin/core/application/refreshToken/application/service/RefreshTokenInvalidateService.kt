package core.application.refreshToken.application.service

import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.inbound.RefreshTokenInvalidator
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class RefreshTokenInvalidateService(
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
) : RefreshTokenInvalidator {
    override fun destroyRefreshToken(memberId: MemberId) {
        refreshTokenPersistencePort.deleteByMemberId(memberId.value)
    }

    override fun destroyByTokenHash(tokenHash: String) {
        var row = refreshTokenPersistencePort.lockByTokenHash(tokenHash) ?: return
        val owner = row.memberId
        val now = Instant.now()
        // Follow the recorded session even if unlink granted a successor after this token rotated.
        val visited = mutableSetOf<String>()
        while (visited.add(row.tokenHash)) {
            refreshTokenPersistencePort.deleteByTokenHash(row.tokenHash)
            if (row.isExpired(now) || !row.isWithinGrace(now, RefreshToken.GRACE_SECONDS)) return
            val nextHash = row.successorTokenHash ?: return
            row = refreshTokenPersistencePort.lockByTokenHash(nextHash) ?: return
            if (row.memberId != owner) return
        }
    }

    override fun destroyOtherDevices(
        memberId: MemberId,
        keepTokenHash: String,
    ) {
        refreshTokenPersistencePort
            .findAllByMemberId(memberId.value)
            .filter { it.tokenHash != keepTokenHash }
            .forEach { refreshTokenPersistencePort.deleteByTokenHash(it.tokenHash) }
    }
}

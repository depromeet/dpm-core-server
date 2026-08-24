package core.persistence.refreshToken.repository

import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import core.entity.refreshToken.RefreshTokenEntity
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class RefreshTokenRepository(
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
) : RefreshTokenPersistencePort {
    override fun save(refreshToken: RefreshToken): RefreshToken =
        refreshTokenJpaRepository
            .save(RefreshTokenEntity.from(refreshToken))
            .toDomain(refreshToken.plainToken)

    override fun findByTokenHash(tokenHash: String): RefreshToken? =
        refreshTokenJpaRepository.findByTokenHash(tokenHash)?.toDomain()

    override fun findAllByMemberId(memberId: Long): List<RefreshToken> =
        refreshTokenJpaRepository.findAllByMemberId(memberId).map { it.toDomain() }

    override fun markRotated(
        tokenHash: String,
        rotatedAt: Instant,
    ) {
        refreshTokenJpaRepository.markRotated(tokenHash, rotatedAt)
    }

    override fun deleteByTokenHash(tokenHash: String) {
        refreshTokenJpaRepository.deleteByTokenHash(tokenHash)
    }

    override fun deleteByMemberId(memberId: Long) {
        refreshTokenJpaRepository.deleteByMemberId(memberId)
    }

    override fun deleteByMemberIdAndDeviceId(
        memberId: Long,
        deviceId: String,
    ) {
        refreshTokenJpaRepository.deleteByMemberIdAndDeviceId(memberId, deviceId)
    }

    override fun deleteExpired(now: Instant): Int = refreshTokenJpaRepository.deleteExpired(now)

    override fun deleteRotatedBefore(threshold: Instant): Int = refreshTokenJpaRepository.deleteRotatedBefore(threshold)
}

package core.persistence.refreshToken.repository

import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import core.entity.refreshToken.RefreshTokenEntity
import org.springframework.stereotype.Repository

@Repository
class RefreshTokenRepository(
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
) : RefreshTokenPersistencePort {
    override fun save(refreshToken: RefreshToken): RefreshToken =
        refreshTokenJpaRepository.save(RefreshTokenEntity.from(refreshToken)).toDomain()

    override fun findByMemberId(memberId: Long): RefreshToken? =
        refreshTokenJpaRepository.findByMemberId(memberId)?.toDomain()

    override fun findByToken(token: String): RefreshToken? = refreshTokenJpaRepository.findByToken(token)?.toDomain()

    override fun deleteByMemberId(memberId: Long) = refreshTokenJpaRepository.deleteByMemberId(memberId)
}

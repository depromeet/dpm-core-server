package com.server.dpmcore.refreshToken.infrastructure.repository

import com.server.dpmcore.refreshToken.domain.model.RefreshToken
import com.server.dpmcore.refreshToken.domain.port.outbound.RefreshTokenPersistencePort
import com.server.dpmcore.refreshToken.infrastructure.entity.RefreshTokenEntity
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

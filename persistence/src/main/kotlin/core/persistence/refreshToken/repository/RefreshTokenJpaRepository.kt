package core.persistence.refreshToken.repository

import core.entity.refreshToken.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, Long> {
    fun findByMemberId(memberId: Long): RefreshTokenEntity?

    fun findByToken(token: String): RefreshTokenEntity?

    fun deleteByMemberId(memberId: Long)
}

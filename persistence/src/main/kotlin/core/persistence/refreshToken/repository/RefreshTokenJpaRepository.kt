package core.persistence.refreshToken.repository

import core.persistence.refreshToken.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, Long> {
    fun findByMemberId(memberId: Long): RefreshTokenEntity?

    fun findByToken(token: String): RefreshTokenEntity?

    fun deleteByMemberId(memberId: Long)
}

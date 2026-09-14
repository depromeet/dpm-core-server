package core.persistence.refreshToken.repository

import core.entity.refreshToken.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, Long> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?

    fun findAllByMemberId(memberId: Long): List<RefreshTokenEntity>

    fun deleteByTokenHash(tokenHash: String)

    fun deleteByMemberId(memberId: Long)

    fun deleteByMemberIdAndDeviceId(
        memberId: Long,
        deviceId: String,
    )

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshTokenEntity t set t.rotatedAt = :rotatedAt where t.tokenHash = :tokenHash")
    fun markRotated(
        @Param("tokenHash") tokenHash: String,
        @Param("rotatedAt") rotatedAt: Instant,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshTokenEntity t where t.expiresAt < :now")
    fun deleteExpired(
        @Param("now") now: Instant,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshTokenEntity t where t.rotatedAt is not null and t.rotatedAt < :threshold")
    fun deleteRotatedBefore(
        @Param("threshold") threshold: Instant,
    ): Int
}

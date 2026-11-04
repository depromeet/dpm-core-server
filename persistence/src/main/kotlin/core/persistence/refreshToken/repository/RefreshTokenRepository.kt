package core.persistence.refreshToken.repository

import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import core.entity.refreshToken.RefreshTokenEntity
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.table
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class RefreshTokenRepository(
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
    private val dsl: DSLContext,
) : RefreshTokenPersistencePort {
    override fun save(refreshToken: RefreshToken): RefreshToken =
        refreshTokenJpaRepository
            .save(RefreshTokenEntity.from(refreshToken))
            .toDomain(refreshToken.plainToken)

    override fun findByTokenHash(tokenHash: String): RefreshToken? =
        refreshTokenJpaRepository.findByTokenHash(tokenHash)?.toDomain()

    override fun lockByTokenHash(tokenHash: String): RefreshToken? {
        // JPA의 1차 캐시와 MySQL 반복 읽기 스냅샷을 피하고 잠금 후 최신 행을 읽는다.
        val tokenId = field(name("token_id"), Long::class.java)
        val memberId = field(name("member_id"), Long::class.java)
        val hash = field(name("token_hash"), String::class.java)
        val deviceId = field(name("device_id"), String::class.java)
        val issuedAt = field(name("issued_at"), LocalDateTime::class.java)
        val expiresAt = field(name("expires_at"), LocalDateTime::class.java)
        val rotatedAt = field(name("rotated_at"), LocalDateTime::class.java)
        val tokenJti = field(name("token_jti"), String::class.java)
        val issuedMemberId = field(name("issued_member_id"), Long::class.java)
        val successor = field(name("successor_token_hash"), String::class.java)
        return dsl.select(
            tokenId, memberId, hash, deviceId, issuedAt, expiresAt, rotatedAt, tokenJti, issuedMemberId, successor,
        )
            .from(table(name("refresh_tokens"))).where(hash.eq(tokenHash)).forUpdate()
            .fetchOne { row ->
                RefreshToken(
                    tokenId = row[tokenId],
                    memberId = MemberId(requireNotNull(row[memberId])),
                    tokenHash = requireNotNull(row[hash]),
                    deviceId = row[deviceId],
                    issuedAt = requireNotNull(row[issuedAt]).toInstant(ZoneOffset.UTC),
                    expiresAt = requireNotNull(row[expiresAt]).toInstant(ZoneOffset.UTC),
                    rotatedAt = row[rotatedAt]?.toInstant(ZoneOffset.UTC),
                    tokenJti = row[tokenJti],
                    issuedMemberId = row[issuedMemberId],
                    successorTokenHash = row[successor],
                )
            }
    }

    override fun findAllByMemberId(memberId: Long): List<RefreshToken> =
        refreshTokenJpaRepository.findAllByMemberId(memberId).map { it.toDomain() }

    override fun markRotated(
        tokenHash: String,
        rotatedAt: Instant,
    ) {
        refreshTokenJpaRepository.markRotated(tokenHash, rotatedAt)
    }

    override fun linkSuccessor(
        tokenHash: String,
        successorTokenHash: String,
    ) {
        check(
            dsl.execute(
                "UPDATE refresh_tokens SET successor_token_hash = ? " +
                    "WHERE token_hash = ? AND successor_token_hash IS NULL",
                successorTokenHash, tokenHash,
            ) == 1,
        )
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

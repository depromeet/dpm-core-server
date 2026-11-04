package core.entity.refreshToken

import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_rt_token_hash", columnNames = ["token_hash"]),
    ],
    indexes = [
        Index(name = "idx_rt_member_id", columnList = "member_id"),
        Index(name = "idx_rt_member_device", columnList = "member_id, device_id"),
        Index(name = "idx_rt_expires_at", columnList = "expires_at"),
    ],
)
class RefreshTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id", nullable = false, updatable = false)
    val id: Long = 0L,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    val tokenHash: String,
    /** Legacy plaintext column retained for schema compatibility; new tokens are stored only as hashes. */
    @Lob
    @Column(name = "token", columnDefinition = "TEXT")
    val token: String? = null,
    @Column(name = "device_id", length = 128)
    val deviceId: String? = null,
    @Column(name = "issued_at", nullable = false, updatable = false)
    val issuedAt: Instant,
    @Column(name = "expires_at", nullable = false, updatable = false)
    val expiresAt: Instant,
    @Column(name = "rotated_at")
    var rotatedAt: Instant? = null,
    @Column(name = "token_jti", length = 36, updatable = false)
    val tokenJti: String? = null,
    @Column(name = "issued_member_id", updatable = false)
    val issuedMemberId: Long? = null,
    @Column(name = "successor_token_hash", length = 64)
    var successorTokenHash: String? = null,
) {
    /** @param plainToken 방금 발급한 경우에만 전달한다. DB 조회 결과에는 null 이다. */
    fun toDomain(plainToken: String? = null): RefreshToken =
        RefreshToken(
            tokenId = this.id,
            memberId = MemberId(this.memberId),
            tokenHash = this.tokenHash,
            deviceId = this.deviceId,
            issuedAt = this.issuedAt,
            expiresAt = this.expiresAt,
            rotatedAt = this.rotatedAt,
            plainToken = plainToken,
            tokenJti = tokenJti,
            issuedMemberId = issuedMemberId,
            successorTokenHash = successorTokenHash,
        )

    companion object {
        fun from(domain: RefreshToken): RefreshTokenEntity =
            RefreshTokenEntity(
                id = domain.tokenId ?: 0L,
                memberId = domain.memberId.value,
                tokenHash = domain.tokenHash,
                token = null,
                deviceId = domain.deviceId,
                issuedAt = domain.issuedAt,
                expiresAt = domain.expiresAt,
                rotatedAt = domain.rotatedAt,
                tokenJti = domain.tokenJti,
                issuedMemberId = domain.issuedMemberId,
                successorTokenHash = domain.successorTokenHash,
            )
    }
}

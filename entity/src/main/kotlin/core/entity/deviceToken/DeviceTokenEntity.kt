package core.entity.deviceToken

import core.domain.deviceToken.aggregate.DeviceToken
import core.domain.deviceToken.vo.DeviceTokenId
import core.domain.member.vo.MemberId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "device_tokens")
class DeviceTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_token_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(nullable = false)
    var token: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,
    @Column(name = "updated_at")
    var updatedAt: Instant? = null,
) {
    fun toDomain(): DeviceToken =
        DeviceToken(
            id = DeviceTokenId(this.id),
            memberId = MemberId(this.memberId),
            token = this.token,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
        )

    companion object {
        fun from(deviceToken: DeviceToken): DeviceTokenEntity =
            DeviceTokenEntity(
                id = deviceToken.id?.value ?: 0L,
                memberId = deviceToken.memberId.value,
                token = deviceToken.token,
                createdAt = deviceToken.createdAt,
                updatedAt = deviceToken.updatedAt,
            )
    }
}

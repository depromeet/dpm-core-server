package core.entity.notification

import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.NotificationToken
import core.domain.notification.vo.NotificationTokenId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(
    name = "notification_tokens",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_notification_token_member_token", columnNames = ["member_id", "token"]),
    ],
    indexes = [
        Index(name = "idx_notification_token_member_id", columnList = "member_id"),
        Index(name = "idx_notification_token_token", columnList = "token"),
    ],
)
class NotificationTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_token_id", nullable = false, updatable = false)
    val id: Long = 0L,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "token", nullable = false, length = 255)
    val token: String,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,
    @Column(name = "updated_at")
    val updatedAt: Instant? = null,
) {
    fun toDomain(): NotificationToken =
        NotificationToken(
            id = NotificationTokenId(this.id),
            memberId = MemberId(this.memberId),
            token = this.token,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
        )

    companion object {
        fun from(domain: NotificationToken): NotificationTokenEntity =
            NotificationTokenEntity(
                id = domain.id?.value ?: 0L,
                memberId = domain.memberId.value,
                token = domain.token,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}

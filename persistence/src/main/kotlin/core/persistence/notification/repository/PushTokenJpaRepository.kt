package core.persistence.notification.repository

import core.entity.notification.NotificationTokenEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PushTokenJpaRepository : JpaRepository<NotificationTokenEntity, Long> {
    fun findByMemberId(memberId: Long): List<NotificationTokenEntity>

    fun findByMemberIdAndToken(
        memberId: Long,
        token: String,
    ): NotificationTokenEntity?

    fun deleteByToken(token: String)

    fun deleteByMemberId(memberId: Long)

    fun findByMemberIdIn(memberIds: List<Long>): List<NotificationTokenEntity>
}

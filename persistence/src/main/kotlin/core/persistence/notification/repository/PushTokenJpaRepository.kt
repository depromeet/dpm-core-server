package core.persistence.notification.repository

import core.domain.member.vo.MemberId
import core.entity.notification.NotificationTokenEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PushTokenJpaRepository : JpaRepository<NotificationTokenEntity, Long> {
    fun findByMemberId(memberId: Long): List<NotificationTokenEntity>

    fun findByMemberIdAndToken(
        memberId: Long,
        token: String,
    ): NotificationTokenEntity?

    fun deleteByToken(token: String)

    fun deleteByMemberId(memberId: MemberId)

    fun findByMemberIdIn(memberIds: List<MemberId>): List<NotificationTokenEntity>
}

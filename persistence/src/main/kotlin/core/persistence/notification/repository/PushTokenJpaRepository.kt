package core.persistence.notification.repository

import core.entity.notification.NotificationTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PushTokenJpaRepository : JpaRepository<NotificationTokenEntity, Long> {
    fun findByMemberId(memberId: Long): List<NotificationTokenEntity>

    fun findByMemberIdAndToken(
        memberId: Long,
        token: String,
    ): NotificationTokenEntity?

    fun deleteByToken(token: String)

    fun deleteByMemberId(memberId: Long)

    @Query(
        """select distinct t.token from NotificationTokenEntity t where t.memberId in :memberIds
        and not exists (select o.id from NotificationTokenEntity o
            where o.token = t.token and o.memberId not in :memberIds)""",
    )
    fun findTokensForRecipients(
        @Param("memberIds") memberIds: List<Long>,
    ): List<String>

    fun findByMemberIdIn(memberIds: List<Long>): List<NotificationTokenEntity>
}

package core.persistence.notification.repository

import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.NotificationToken
import core.domain.notification.port.outbound.NotificationPersistencePort
import core.domain.notification.vo.NotificationTokenId
import core.entity.notification.NotificationTokenEntity
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class NotificationRepository(
    private val pushTokenJpaRepository: PushTokenJpaRepository,
) : NotificationPersistencePort {
    override fun save(notificationToken: NotificationToken): NotificationToken =
        pushTokenJpaRepository.save(NotificationTokenEntity.from(notificationToken))
            .toDomain()

    override fun findByMemberId(memberId: MemberId): List<NotificationToken> =
        pushTokenJpaRepository
            .findByMemberId(memberId.value)
            .map { it.toDomain() }

    override fun findByMemberIdAndToken(
        memberId: MemberId,
        token: String,
    ): NotificationToken? =
        pushTokenJpaRepository
            .findByMemberIdAndToken(
                memberId = memberId.value,
                token = token,
            )?.toDomain()

    @Transactional
    override fun deleteByToken(token: String) {
        pushTokenJpaRepository.deleteByToken(token)
    }

    @Transactional
    override fun deleteByMemberId(memberId: MemberId) {
        pushTokenJpaRepository.deleteByMemberId(memberId.value)
    }

    override fun findById(id: NotificationTokenId): NotificationToken? =
        pushTokenJpaRepository
            .findById(id.value)
            .orElse(null)
            ?.toDomain()

    override fun findByMemberIdIn(memberIds: List<MemberId>): List<NotificationToken> =
        pushTokenJpaRepository
            .findByMemberIdIn(memberIds.map { it.value })
            .map { it.toDomain() }
}

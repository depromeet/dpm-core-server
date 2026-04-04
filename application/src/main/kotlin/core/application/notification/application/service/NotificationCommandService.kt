package core.application.notification.application.service

import core.application.notification.application.exception.NotificationTokenNotFoundException
import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.NotificationToken
import core.domain.notification.enums.NotificationMessage
import core.domain.notification.port.inbound.NotificationCommandUseCase
import core.domain.notification.port.outbound.NotificationPersistencePort
import core.persistence.expo.ExpoPriority
import core.persistence.expo.ExpoPushClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationCommandService(
    private val expoPushClient: ExpoPushClient,
    private val notificationRepository: NotificationPersistencePort,
) : NotificationCommandUseCase {
    companion object {
        private val logger = LoggerFactory.getLogger(NotificationCommandService::class.java)
    }

    @Transactional
    override fun registerPushToken(
        memberId: MemberId,
        token: String,
    ): NotificationToken {
        val notificationToken = NotificationToken.create(memberId = memberId, token = token)
        val savedToken = notificationRepository.save(notificationToken)

        return savedToken
    }

    @Transactional
    override fun deletePushToken(
        memberId: MemberId,
        token: String,
    ) {
        val existingToken =
            notificationRepository.findByMemberIdAndToken(memberId, token)
                ?: throw NotificationTokenNotFoundException()
        notificationRepository.deleteByToken(token)
    }

    @Transactional
    override fun deleteAllPushTokens(memberId: MemberId) {
        val tokens = notificationRepository.findByMemberId(memberId)
        if (tokens.isNotEmpty()) {
            notificationRepository.deleteByMemberId(memberId)
        } else {
            throw NotificationTokenNotFoundException()
        }
    }

    fun sendPushNotification(
        memberId: MemberId,
        messageType: NotificationMessage,
        variables: Map<String, Any> = emptyMap(),
        data: Map<String, Any>? = null,
    ) {
        val (title, body) = messageType.formatWithTitle(variables)
        sendPushNotificationInternal(memberId, title, body, data)
    }

    fun sendCustomPushNotification(
        memberId: MemberId,
        title: String,
        body: String,
        data: Map<String, Any>? = null,
    ) {
        sendPushNotificationInternal(memberId, title, body, data)
    }

    @Async
    fun sendPushNotificationToMembers(
        memberIds: List<MemberId>,
        messageType: NotificationMessage,
        variables: Map<String, Any> = emptyMap(),
        data: Map<String, Any>? = null,
        expoPriority: ExpoPriority = ExpoPriority.NORMAL,
    ) {
        if (memberIds.size == 0) {
            logger.error("알림 발송에 입력된 멤버 수가 0명입니다.")
            return
        }
        val (title, body) = messageType.formatWithTitle(variables)

        val allTokens = notificationRepository.findByMemberIdIn(memberIds).map { it.token }

        if (allTokens.isEmpty()) {
            logger.error("조회된 토큰이 없습니다.")
            return
        }

        expoPushClient.sendBatch(
            tokens = allTokens,
            title = title,
            body = body,
            data = data,
            priority = expoPriority,
        )

        logger.info("Push notifications sent to ${allTokens.size} device(s)")
    }

    private fun sendPushNotificationInternal(
        memberId: MemberId,
        title: String,
        body: String,
        data: Map<String, Any>?,
    ) {
        val tokens: List<NotificationToken> =
            notificationRepository.findByMemberId(memberId)

        tokens.forEach { pushToken ->
            expoPushClient.send(
                token = pushToken.token,
                title = title,
                body = body,
                data = data,
                priority = ExpoPriority.NORMAL,
            )
        }

        logger.info("Push notifications sent to ${tokens.size} device(s) for member ${memberId.value}")
    }
}

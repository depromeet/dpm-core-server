package core.application.notification.application.service

import core.domain.member.vo.MemberId
import core.domain.notification.enums.NotificationMessageType
import core.domain.notification.port.outbound.NotificationPersistencePort
import core.persistence.expo.ExpoPriority
import core.persistence.expo.ExpoPushClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AbsenceReasonPushService(
    private val tokens: NotificationPersistencePort,
    private val expo: ExpoPushClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun send(
        recipients: List<MemberId>,
        type: NotificationMessageType,
        variables: Map<String, Any>,
        data: Map<String, Any>,
    ) {
        val targets = tokens.findTokensForRecipients(recipients)
        if (targets.isEmpty()) return
        val (title, body) = type.formatWithTitle(variables)
        val payload = NotificationCommandService.mergePushData(type, data)
        for (batch in targets.chunked(100)) {
            val tickets = expo.sendBatch(batch, title, body, payload, ExpoPriority.NORMAL, retryTransport = false)
            // Expo returns tickets in request order. Only explicitly rate-limited devices can be retried safely.
            val throttled =
                batch.filterIndexed { index, _ ->
                    val ticket = tickets.getOrNull(index)
                    ticket?.status == "error" && ticket.details?.get("error") == "MessageRateExceeded"
                }
            var failures = batch.size - tickets.count { it?.status == "ok" }
            if (throttled.isNotEmpty()) {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    logger.warn("Absence push retry interrupted: type={}, pending={}", type, throttled.size)
                    return
                }
                val retried =
                    expo.sendBatch(
                        throttled,
                        title,
                        body,
                        payload,
                        ExpoPriority.NORMAL,
                        retryTransport = false,
                    )
                failures -= retried.count { it?.status == "ok" }
            }
            if (failures > 0) {
                logger.warn("Absence push tickets failed: type={}, failed={}, total={}", type, failures, batch.size)
            }
        }
    }
}

package core.persistence.expo

import core.domain.notification.aggregate.NotificationExpoMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Component
class ExpoPushClient(
    @Value("\${expo.push.url:https://exp.host/--/api/v2/push/send}")
    private val expoPushUrl: String,
    @Value("\${expo.push.access-token:#{null}}")
    private val expoAccessToken: String?,
) {
    private val logger = LoggerFactory.getLogger(ExpoPushClient::class.java)

    private val webClient: WebClient =
        WebClient
            .builder()
            .baseUrl(expoPushUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .apply {
                if (!expoAccessToken.isNullOrBlank()) {
                    it.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $expoAccessToken")
                }
            }.build()

    fun send(
        token: String,
        title: String,
        body: String,
        data: Map<String, Any>? = null,
        priority: ExpoPriority,
        sound: String = "default",
        badge: Int? = null,
    ): ExpoPushTicket? =
        try {
            val message =
                NotificationExpoMessage(
                    to = token,
                    title = title,
                    body = body,
                    data = data,
                    priority = priority.value,
                    sound = sound,
                    badge = badge,
                )

            val response: ExpoPushResponse? = sendMessage(listOf(message))

            val ticket = response?.data?.firstOrNull()

            when (ticket?.status) {
                "ok" -> {
                    logger.info("푸시 알림 발송 성공 : id=${ticket.id}, token=$token")
                    ticket
                }
                else -> {
                    logger.error(
                        "푸시 알림 발송 실패 : message=${ticket?.message}, " +
                            "details=${ticket?.details}, token=$token",
                    )
                    ticket
                }
            }
        } catch (e: WebClientResponseException) {
            logger.error("Expo API 오류 : ${e.statusCode} - ${e.responseBodyAsString}", e)
            null
        } catch (e: Exception) {
            logger.error("알림 발송 실패 : ", e)
            null
        }

    fun sendBatch(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, Any>? = null,
        priority: ExpoPriority,
    ): List<ExpoPushTicket?> {
        if (tokens.isEmpty()) {
            return emptyList()
        }

        return try {
            val messages =
                tokens.map { token ->
                    NotificationExpoMessage(
                        to = token,
                        title = title,
                        body = body,
                        data = data,
                        priority = priority.value,
                        sound = "default",
                    )
                }

            val response = sendMessage(messages)

            response?.data ?: emptyList()
        } catch (e: Exception) {
            logger.error("배치 알림 발송 실패 : ", e)
            List(tokens.size) { null }
        }
    }

    fun sendMessage(message: Any): ExpoPushResponse? =
        webClient
            .post()
            .bodyValue(message)
            .retrieve()
            .bodyToMono(ExpoPushResponse::class.java)
            .onErrorResume { error ->
                if (error is WebClientResponseException) {
                    logger.error("Expo API 상세 응답: ${error.responseBodyAsString}")
                }
                Mono.error(error)
            }
            .retryWhen(
                Retry
                    .backoff(3, Duration.ofMillis(100))
                    .maxBackoff(Duration.ofSeconds(2)),
            ).block(Duration.ofSeconds(10))
}

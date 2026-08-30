package core.application.common.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * ERROR 로그를 Discord 웹훅으로 보내는 Logback appender.
 *
 * Logback 이 직접 생성하므로 스프링 빈을 주입받을 수 없다. 설정값은 logback-spring.xml 의
 * `springProperty` 로 넘어오고, HTTP 는 스프링/리액터 대신 JDK 클라이언트를 쓴다.
 * (WebClient 는 내부적으로 로그를 남겨 알림 → 로그 → 알림 재귀에 빠질 위험이 있다.)
 *
 * 알림 전송이 다른 기능에 영향을 주지 않도록 세 가지를 지킨다.
 * - 호출 스레드에서 직접 보내지 않는다. AsyncAppender 의 전용 워커에서만 실행된다.
 * - 어떤 예외도 밖으로 던지지 않는다. 실패는 [addError] 로만 남긴다. (logger 를 쓰면 재귀한다)
 * - 전송은 타임아웃을 두고 재시도하지 않는다. 디스코드가 느려도 큐만 쌓이다 버려질 뿐이다.
 */
class DiscordWebhookAppender : AppenderBase<ILoggingEvent>() {
    var webhookUrl: String? = null
    var appName: String = "application"
    var profile: String = "unknown"
    var cooldownSeconds: Long = 300
    var maxPerMinute: Int = 20
    var stackTraceLines: Int = 20
    var includePackages: String = ""
    var connectTimeoutMillis: Long = 2_000
    var readTimeoutMillis: Long = 3_000
    var excludedExceptions: String = ""
    var excludedLoggers: String = ""

    private lateinit var httpClient: HttpClient
    private lateinit var endpoint: URI
    private lateinit var alertFilter: ErrorAlertFilter
    private lateinit var throttler: ErrorAlertThrottler
    private lateinit var formatter: DiscordMessageFormatter

    override fun start() {
        val url = webhookUrl?.trim()
        if (url.isNullOrEmpty()) {
            addError("webhookUrl 이 비어 있어 Discord 알림을 시작하지 않는다.")
            return
        }
        endpoint =
            runCatching { URI.create(url) }.getOrElse {
                addError("webhookUrl 형식이 올바르지 않다.", it)
                return
            }

        httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .build()
        alertFilter = ErrorAlertFilter(excludedExceptions, excludedLoggers)
        throttler = ErrorAlertThrottler(Duration.ofSeconds(cooldownSeconds).toMillis(), maxPerMinute)
        formatter = DiscordMessageFormatter(appName, profile, stackTraceLines, includePackages)

        super.start()
    }

    override fun append(event: ILoggingEvent) {
        if (!isStarted || sending.get()) return

        try {
            if (!alertFilter.shouldAlert(event)) return

            val decision = throttler.tryAcquire(signatureOf(event), event.timeStamp)
            if (decision !is ErrorAlertThrottler.Decision.Send) return

            sending.set(true)
            send(formatter.format(event, decision.suppressedCount))
        } catch (throwable: Throwable) {
            addError("Discord 알림 전송에 실패했다.", throwable)
        } finally {
            sending.set(false)
        }
    }

    private fun send(payload: String) {
        val request =
            HttpRequest
                .newBuilder(endpoint)
                .timeout(Duration.ofMillis(readTimeoutMillis))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, Charsets.UTF_8))
                .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= HTTP_BAD_REQUEST) {
            addError("Discord 웹훅이 ${response.statusCode()} 를 반환했다: ${response.body().take(RESPONSE_LOG_LIMIT)}")
        }
    }

    /**
     * 같은 지점에서 반복되는 에러를 하나로 묶기 위한 키.
     *
     * 예외가 없는 로그는 메시지로 구분할 수밖에 없는데, 메시지에는 토큰이나 ID 가 섞여 들어온다.
     * (예: 푸시 발송 실패 로그는 실패한 토큰마다 문자열이 다르다)
     * 그대로 쓰면 같은 장애가 매번 다른 시그니처가 되어 쿨다운을 통째로 우회하므로 가변 부분을 지운다.
     */
    private fun signatureOf(event: ILoggingEvent): String {
        val proxy = ThrowableProxies.chain(event.throwableProxy).firstOrNull()
        val exceptionName =
            proxy?.className
                ?: normalize(event.formattedMessage.orEmpty()).take(SIGNATURE_MESSAGE_LENGTH)
        val topFrame = proxy?.stackTraceElementProxyArray?.firstOrNull()?.steAsString.orEmpty()
        return "${event.loggerName}|$exceptionName|$topFrame"
    }

    private fun normalize(message: String): String =
        message
            .replace(LONG_TOKEN_PATTERN, "*")
            .replace(NUMBER_PATTERN, "#")

    companion object {
        private const val HTTP_BAD_REQUEST = 400
        private const val RESPONSE_LOG_LIMIT = 200
        private const val SIGNATURE_MESSAGE_LENGTH = 100

        /** 토큰, UUID 처럼 요청마다 달라지는 긴 문자열 */
        private val LONG_TOKEN_PATTERN = Regex("[A-Za-z0-9_-]{16,}")
        private val NUMBER_PATTERN = Regex("\\d+")

        /** 전송 과정에서 발생한 로그를 다시 잡아 무한 재귀하는 것을 막는다. */
        private val sending = ThreadLocal.withInitial { false }
    }
}

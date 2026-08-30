package core.application.common.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 필터 - 스로틀러 - 포매터 - 전송이 실제로 이어지는지 확인한다.
 * 알림 전송은 어떤 경우에도 애플리케이션으로 예외를 던지면 안 되므로, 실패 상황까지 함께 검증한다.
 */
class DiscordWebhookAppenderTest {
    private lateinit var server: HttpServer
    private val receivedBodies = CopyOnWriteArrayList<String>()

    @BeforeEach
    fun startStubServer() {
        receivedBodies.clear()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/webhook") { exchange ->
            receivedBodies.add(exchange.requestBody.readBytes().decodeToString())
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
        server.start()
    }

    @AfterEach
    fun stopStubServer() {
        server.stop(0)
    }

    @Test
    fun `5xx 에러를 웹훅으로 보낸다`() {
        val appender = appender()

        appender.doAppend(event("Exception: IllegalStateException - boom", IllegalStateException("boom")))

        assertThat(receivedBodies).hasSize(1)
        assertThat(receivedBodies.first()).contains("IllegalStateException")
    }

    @Test
    fun `4xx 비즈니스 에러는 보내지 않는다`() {
        val appender = appender()

        appender.doAppend(event("SESSION_NOT_FOUND Exception SESSION-404-01: 세션을 찾을 수 없습니다"))

        assertThat(receivedBodies).isEmpty()
    }

    @Test
    fun `같은 에러가 쏟아져도 쿨다운 안에서는 한 번만 보낸다`() {
        val appender = appender()

        repeat(50) { appender.doAppend(event("터졌다", IllegalStateException("boom"))) }

        assertThat(receivedBodies).hasSize(1)
    }

    @Test
    fun `메시지에 섞인 토큰이 달라도 같은 장애로 묶어 한 번만 보낸다`() {
        // 푸시 발송 실패 로그처럼 예외 없이 메시지만 남고, 그 메시지에 매번 다른 토큰이 섞이는 경우
        val appender = appender()

        repeat(30) { index ->
            appender.doAppend(event("푸시 알림 발송 실패 : token=ExponentPushToken_abcdefghij$index"))
        }

        assertThat(receivedBodies).hasSize(1)
    }

    @Test
    fun `웹훅 주소가 죽어 있어도 예외를 던지지 않는다`() {
        val appender = appender(url = "http://127.0.0.1:1/webhook")

        appender.doAppend(event("터졌다", IllegalStateException("boom")))

        assertThat(receivedBodies).isEmpty()
    }

    @Test
    fun `웹훅 주소가 비어 있으면 appender 가 시작되지 않는다`() {
        val appender = DiscordWebhookAppender().apply { webhookUrl = "" }

        appender.start()

        assertThat(appender.isStarted).isFalse()
    }

    private fun appender(url: String = "http://127.0.0.1:${server.address.port}/webhook"): DiscordWebhookAppender =
        DiscordWebhookAppender()
            .apply {
                context = LoggerContext()
                webhookUrl = url
                appName = "dpm-core-server"
                profile = "test"
                cooldownSeconds = 300
                maxPerMinute = 20
                includePackages = "core."
                connectTimeoutMillis = 500
                readTimeoutMillis = 1_000
                excludedExceptions = "org.springframework.web.HttpRequestMethodNotSupportedException"
            }.also { it.start() }

    private fun event(
        message: String,
        throwable: Throwable? = null,
    ): LoggingEvent {
        val loggerName = "core.application.common.exception.GlobalExceptionHandler"
        return LoggingEvent(
            loggerName,
            LoggerContext().getLogger(loggerName),
            Level.ERROR,
            message,
            throwable,
            emptyArray(),
        ).apply { mdcPropertyMap = emptyMap() }
    }
}

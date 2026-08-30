package core.application.common.logging

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.filter.ThresholdFilter
import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.logging.LoggingInitializationContext
import org.springframework.boot.logging.logback.LogbackLoggingSystem
import org.springframework.mock.env.MockEnvironment
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * logback-spring.xml 의 값이 실제로 appender 에 꽂히는지 확인한다.
 *
 * `<webhookUrl>` 같은 태그 이름은 setter 이름과 문자열로만 연결돼 있어, 하나만 틀려도
 * 애플리케이션은 정상 기동하고 알림만 조용히 사라진다. 배포 후에야 발견되는 종류의 실수라 여기서 잡는다.
 */
class LogbackDiscordConfigurationTest {
    private lateinit var server: HttpServer
    private lateinit var loggingSystem: LogbackLoggingSystem
    private val receivedBodies = CopyOnWriteArrayList<String>()

    @BeforeEach
    fun initializeLoggingSystem() {
        receivedBodies.clear()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/webhook") { exchange ->
            receivedBodies.add(exchange.requestBody.readBytes().decodeToString())
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
        server.start()

        val environment =
            MockEnvironment().apply {
                setActiveProfiles("dev")
                setProperty("logging.discord.webhook-url", "http://127.0.0.1:${server.address.port}/webhook")
                setProperty("logging.discord.app-name", "dpm-core-server")
                setProperty("logging.discord.cooldown-seconds", "300")
                setProperty("logging.discord.max-per-minute", "20")
                setProperty("logging.discord.stack-trace-lines", "20")
                setProperty("logging.discord.queue-size", "512")
                setProperty("logging.discord.connect-timeout-millis", "500")
                setProperty("logging.discord.read-timeout-millis", "1000")
                setProperty("logging.discord.excluded-exceptions", "")
                setProperty("logging.discord.excluded-loggers", "")
            }

        loggingSystem = LogbackLoggingSystem(javaClass.classLoader)
        loggingSystem.beforeInitialize()
        loggingSystem.initialize(LoggingInitializationContext(environment), "classpath:logback-spring.xml", null)
    }

    @AfterEach
    fun restoreLoggingSystem() {
        // 로그백 컨텍스트는 JVM 전역이다. 붙여둔 appender 를 떼어내지 않으면 뒤이어 도는 테스트의 로그가 여기로 흘러든다.
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.getAppender("DISCORD_ASYNC")?.let {
            root.detachAppender(it)
            it.stop()
        }
        loggingSystem.cleanUp()
        server.stop(0)
    }

    @Test
    fun `dev 프로필에서 Discord appender 가 루트 로거에 붙는다`() {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

        assertThat(root.getAppender("DISCORD_ASYNC")).isNotNull()
        assertThat(root.getAppender("CONSOLE")).isNotNull()
    }

    @Test
    fun `설정값이 주입되어 ERROR 로그가 실제로 웹훅까지 전달된다`() {
        LoggerFactory.getLogger(javaClass).error("설정 배선 확인", IllegalStateException("boom"))

        // AsyncAppender 의 전용 워커가 보내므로 도착할 때까지 기다린다
        awaitDelivery()
        assertThat(receivedBodies.first())
            .contains("dpm-core-server")
            .contains("IllegalStateException")
            .contains("boom")
    }

    @Test
    fun `ERROR 만 큐에 들어가도록 필터가 AsyncAppender 에 붙어 있다`() {
        // 필터가 안쪽 appender 에 붙으면 INFO/DEBUG 까지 큐를 거쳐, 호출 스레드가 비용을 물고 ERROR 가 밀려난다
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val async = root.getAppender("DISCORD_ASYNC") as AsyncAppender

        val threshold = async.copyOfAttachedFiltersList.filterIsInstance<ThresholdFilter>()

        assertThat(threshold).hasSize(1)
    }

    @Test
    fun `INFO 로그는 웹훅으로 가지 않는다`() {
        LoggerFactory.getLogger(javaClass).info("평범한 로그")

        Thread.sleep(NEGATIVE_CHECK_MILLIS)
        assertThat(receivedBodies).isEmpty()
    }

    private fun awaitDelivery() {
        val deadline = System.currentTimeMillis() + DELIVERY_TIMEOUT_MILLIS
        while (receivedBodies.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        assertThat(receivedBodies).describedAs("웹훅으로 전달된 알림").isNotEmpty()
    }

    companion object {
        private const val DELIVERY_TIMEOUT_MILLIS = 5_000L
        private const val POLL_INTERVAL_MILLIS = 50L
        private const val NEGATIVE_CHECK_MILLIS = 300L
    }
}

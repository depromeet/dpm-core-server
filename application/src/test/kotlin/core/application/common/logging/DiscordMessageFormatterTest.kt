package core.application.common.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Discord 는 embed description 4096자, field value 1024자를 넘기면 400 을 돌려준다.
 * 그리고 로그 메시지에는 토큰이나 이메일이 그대로 섞여 들어올 수 있어, 채널에 남기기 전에 지워야 한다.
 */
class DiscordMessageFormatterTest {
    private val objectMapper = ObjectMapper()
    private val formatter = DiscordMessageFormatter(appName = "dpm-core-server", profile = "dev", stackTraceLines = 5)

    @Test
    fun `예외 이름과 프로필이 제목에 담긴다`() {
        val embed = embedOf(event("Exception: NullPointerException - null", NullPointerException()))

        assertThat(embed["title"].asText()).contains("dev").contains("NullPointerException")
    }

    @Test
    fun `스택트레이스가 본문에 담긴다`() {
        val embed = embedOf(event("터졌다", IllegalStateException("boom")))

        assertThat(embed["description"].asText())
            .contains("java.lang.IllegalStateException: boom")
            .contains("\tat ")
    }

    @Test
    fun `MDC 의 요청 정보가 필드로 들어간다`() {
        val event =
            event(
                "터졌다",
                NullPointerException(),
                mdc =
                    mapOf(
                        MdcLoggingFilter.HTTP_METHOD to "POST",
                        MdcLoggingFilter.REQUEST_URI to "/v1/sessions/1/attendance",
                        MdcLoggingFilter.REQUEST_ID to "a1b2c3d4",
                    ),
            )

        val fields = embedOf(event)["fields"].associate { it["name"].asText() to it["value"].asText() }

        assertThat(fields["요청"]).isEqualTo("POST /v1/sessions/1/attendance")
        assertThat(fields["requestId"]).isEqualTo("a1b2c3d4")
    }

    @Test
    fun `눌러둔 알림 건수를 함께 표시한다`() {
        val fields =
            embedOf(event("터졌다", NullPointerException()), suppressedCount = 7)["fields"]
                .associate { it["name"].asText() to it["value"].asText() }

        assertThat(fields["생략된 동일 알림"]).isEqualTo("7건")
    }

    @Test
    fun `액세스 토큰과 이메일을 마스킹한다`() {
        val message =
            "인증 실패 Authorization=Bearer abc.def.ghi " +
                "token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature member=user@depromeet.com"

        val description = embedOf(event(message))["description"].asText()

        assertThat(description)
            .doesNotContain("abc.def.ghi")
            .doesNotContain("user@depromeet.com")
            .doesNotContain("eyJhbGciOiJIUzI1NiJ9")
    }

    @Test
    fun `OAuth 콜백 URI 의 인증 코드를 마스킹한다`() {
        val event =
            event(
                "터졌다",
                NullPointerException(),
                mdc =
                    mapOf(
                        MdcLoggingFilter.HTTP_METHOD to "GET",
                        MdcLoggingFilter.REQUEST_URI to "/login/oauth2/code/kakao?code=SECRET_AUTH_CODE&state=abc123",
                    ),
            )

        val request =
            embedOf(event)["fields"]
                .associate { it["name"].asText() to it["value"].asText() }["요청"]

        assertThat(request)
            .doesNotContain("SECRET_AUTH_CODE")
            .doesNotContain("abc123")
            .contains("/login/oauth2/code/kakao")
    }

    @Test
    fun `본문이 Discord 상한을 넘지 않는다`() {
        val deepException = IllegalStateException("x".repeat(5_000))

        val embed = embedOf(event("y".repeat(5_000), deepException))

        assertThat(embed["description"].asText().length).isLessThanOrEqualTo(4_096)
    }

    @Test
    fun `예외가 없는 로그도 처리한다`() {
        val embed = embedOf(event("알림 발송에 실패했습니다"))

        assertThat(embed["description"].asText()).contains("알림 발송에 실패했습니다")
        assertThat(embed["title"].asText()).contains("Error")
    }

    private fun embedOf(
        event: LoggingEvent,
        suppressedCount: Int = 0,
    ): JsonNode = objectMapper.readTree(formatter.format(event, suppressedCount))["embeds"][0]

    private fun event(
        message: String,
        throwable: Throwable? = null,
        mdc: Map<String, String> = emptyMap(),
    ): LoggingEvent {
        val loggerName = "core.application.common.exception.GlobalExceptionHandler"
        return LoggingEvent(
            loggerName,
            LoggerContext().getLogger(loggerName),
            Level.ERROR,
            message,
            throwable,
            emptyArray(),
        ).apply { mdcPropertyMap = mdc }
    }
}

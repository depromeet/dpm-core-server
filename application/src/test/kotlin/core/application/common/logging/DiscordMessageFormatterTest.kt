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
 *
 * 본문 구성(원인 체인 / 우리 코드 프레임)도 함께 검증한다.
 * 스택을 앞에서부터 자르면 우리 호출 경로가 프레임워크 프레임에 밀려 사라지는데, 이건 알림을 봐도 원인을 모르는 상태와 같다.
 */
class DiscordMessageFormatterTest {
    private val objectMapper = ObjectMapper()
    private val formatter =
        DiscordMessageFormatter(
            appName = "dpm-core-server",
            profile = "dev",
            stackTraceLines = 5,
            includePackages = "core.",
        )

    @Test
    fun `예외 이름과 프로필이 제목에 담긴다`() {
        val embed = embedOf(event("Exception: NullPointerException - null", NullPointerException()))

        assertThat(embed["title"].asText()).contains("dev").contains("NullPointerException")
    }

    @Test
    fun `예외 메시지가 본문에 담긴다`() {
        val embed = embedOf(event("터졌다", IllegalStateException("boom")))

        assertThat(embed["description"].asText())
            .contains("터졌다")
            .contains("IllegalStateException: boom")
    }

    @Test
    fun `원인 예외까지 체인으로 이어 보여준다`() {
        val cause = IllegalArgumentException("만료된 코드")
        val event = event("출석 실패", IllegalStateException("처리 실패", cause))

        assertThat(embedOf(event)["description"].asText())
            .contains("원인 체인")
            .contains("IllegalStateException: 처리 실패")
            .contains("IllegalArgumentException: 만료된 코드")
    }

    @Test
    fun `우리 코드 프레임만 남기고 프레임워크 프레임은 줄 수로 접는다`() {
        val exception =
            exceptionWith(
                "core.application.attendance.AttendanceCommandService",
                "org.springframework.web.servlet.DispatcherServlet",
                "org.apache.catalina.core.StandardWrapperValve",
                "java.base.java.lang.Thread",
            )

        val description = embedOf(event("터졌다", exception))["description"].asText()

        assertThat(description)
            .contains("AttendanceCommandService.checkIn:88")
            .doesNotContain("DispatcherServlet")
            .doesNotContain("StandardWrapperValve")
            .contains("나머지 3줄 생략")
    }

    @Test
    fun `우리 코드 프레임이 하나도 없으면 상위 몇 줄이라도 남긴다`() {
        val exception =
            exceptionWith(
                "org.springframework.web.servlet.DispatcherServlet",
                "org.apache.catalina.core.StandardWrapperValve",
            )

        val description = embedOf(event("터졌다", exception))["description"].asText()

        assertThat(description)
            .contains("스택 상위")
            .contains("org.springframework.web.servlet.DispatcherServlet")
    }

    @Test
    fun `원인 예외와 겹치는 프레임을 중복해서 싣지 않는다`() {
        // 원인 예외의 스택은 감싼 예외와 아래쪽 프레임을 공유한다. Java 가 "... N more" 로 줄이는 부분이다.
        val cause = exceptionWith("core.domain.attendance.AttendanceCode")
        val wrapper = IllegalStateException("처리 실패", cause)
        wrapper.stackTrace = cause.stackTrace

        val description = embedOf(event("터졌다", wrapper))["description"].asText()

        assertThat(description.split("AttendanceCode.checkIn:88")).hasSize(2)
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

        val fields = fieldsOf(embedOf(event))

        assertThat(fields["요청"]).isEqualTo("POST /v1/sessions/1/attendance")
        assertThat(fields["requestId"]).isEqualTo("a1b2c3d4")
    }

    @Test
    fun `로그인한 요청이면 memberId 를 함께 보여준다`() {
        val event = event("터졌다", NullPointerException(), mdc = mapOf(MdcLoggingFilter.MEMBER_ID to "1234"))

        assertThat(fieldsOf(embedOf(event))["사용자"]).isEqualTo("memberId 1234")
    }

    @Test
    fun `로그인하지 않은 요청이면 사용자 필드가 아예 빠진다`() {
        val event = event("터졌다", NullPointerException())

        assertThat(fieldsOf(embedOf(event))).doesNotContainKey("사용자")
    }

    @Test
    fun `로그 메시지의 마크다운 문자가 서식으로 해석되지 않는다`() {
        // member_id_value 가 기울임이 되거나 줄머리 # 가 헤딩이 되면 원문을 못 읽는다
        val embed = embedOf(event("member_id_value 없음 *중요* \n# 헤딩", NullPointerException()))

        assertThat(embed["description"].asText())
            .contains("""member\_id\_value""")
            .contains("""\*중요\*""")
            .contains("""\# 헤딩""")
    }

    @Test
    fun `필드 값의 마크다운 문자도 이스케이프한다`() {
        val event =
            event(
                "터졌다",
                NullPointerException(),
                mdc =
                    mapOf(
                        MdcLoggingFilter.HTTP_METHOD to "GET",
                        MdcLoggingFilter.REQUEST_URI to "/v1/member_roles/some_id",
                    ),
            )

        assertThat(fieldsOf(embedOf(event))["요청"]).isEqualTo("""GET /v1/member\_roles/some\_id""")
    }

    @Test
    fun `예외 메시지에 섞인 코드펜스가 블록을 닫지 않는다`() {
        val embed = embedOf(event("쿼리 실패", IllegalStateException("bad sql: ```select 1```")))

        val description = embed["description"].asText()
        // 여는 펜스와 닫는 펜스만 남아야 한다. 본문의 ``` 가 그대로면 블록이 중간에 끊긴다
        assertThat(description.windowed(3).count { it == "```" }).isEqualTo(EXPECTED_FENCE_COUNT)
    }

    @Test
    fun `멘션이 살아나지 않도록 allowed_mentions 를 비워 보낸다`() {
        val payload = objectMapper.readTree(formatter.format(event("@everyone 처리 실패"), 0))

        assertThat(payload["allowed_mentions"]["parse"]).isEmpty()
    }

    @Test
    fun `값이 빈 MDC 는 필드로 만들지 않는다`() {
        val event = event("터졌다", NullPointerException(), mdc = mapOf(MdcLoggingFilter.REQUEST_ID to ""))

        assertThat(fieldsOf(embedOf(event))).doesNotContainKey("requestId")
    }

    @Test
    fun `clientIp 는 알림에 싣지 않는다`() {
        // memberId 가 "누가" 를 대신한다. IP 가 필요한 경우는 requestId 로 콘솔 로그를 찾는다
        val event = event("터졌다", NullPointerException(), mdc = mapOf(MdcLoggingFilter.CLIENT_IP to "10.0.3.117"))

        assertThat(fieldsOf(embedOf(event))).doesNotContainKey("clientIp")
    }

    @Test
    fun `스택트레이스가 비어 있어도 처리한다`() {
        val embed = embedOf(event("터졌다", NullPointerException().apply { stackTrace = emptyArray() }))

        assertThat(embed["description"].asText()).contains("터졌다")
    }

    @Test
    fun `임베드 전체가 Discord 총합 상한을 넘지 않는다`() {
        val deepChain =
            generateSequence(RuntimeException("z".repeat(500))) { RuntimeException("z".repeat(500), it) }
                .take(6)
                .last()
        val event =
            event(
                "m".repeat(5_000),
                deepChain,
                mdc =
                    mapOf(
                        MdcLoggingFilter.HTTP_METHOD to "POST",
                        MdcLoggingFilter.REQUEST_URI to "/v1/x?q=" + "u".repeat(4_000),
                        MdcLoggingFilter.REQUEST_ID to "a1b2c3d4",
                        MdcLoggingFilter.MEMBER_ID to "1234",
                    ),
            )

        val embed = embedOf(event, suppressedCount = 3)
        val total =
            embed["title"].asText().length +
                embed["description"].asText().length +
                embed["footer"]["text"].asText().length +
                embed["fields"].sumOf { it["name"].asText().length + it["value"].asText().length }

        assertThat(total).isLessThanOrEqualTo(EMBED_TOTAL_LIMIT)
    }

    @Test
    fun `눌러둔 알림 건수를 함께 표시한다`() {
        val embed = embedOf(event("터졌다", NullPointerException()), suppressedCount = 7)

        assertThat(embed["footer"]["text"].asText()).contains("동일 알림 7건 생략")
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

        val request = fieldsOf(embedOf(event))["요청"]

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

    private fun fieldsOf(embed: JsonNode): Map<String, String> = embed["fields"].associate { it["name"].asText() to it["value"].asText() }

    /** 프레임 필터링을 확인하려면 스택 구성이 정해져 있어야 한다. 실제 호출로는 재현할 수 없어 직접 심는다. */
    private fun exceptionWith(vararg classNames: String): IllegalStateException =
        IllegalStateException("boom").apply {
            stackTrace =
                classNames
                    .map { StackTraceElement(it, "checkIn", "${it.substringAfterLast('.')}.kt", 88) }
                    .toTypedArray()
        }

    private companion object {
        /** 여는 펜스 + 닫는 펜스가 블록마다 하나씩. 원인 체인 블록과 프레임 블록 두 개. */
        private const val EXPECTED_FENCE_COUNT = 4
        private const val EMBED_TOTAL_LIMIT = 6_000
    }

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

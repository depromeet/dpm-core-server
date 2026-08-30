package core.application.common.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode
import core.application.security.oauth.exception.InvalidAccessTokenException
import core.application.security.oauth.exception.JwtExceptionCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.HttpRequestMethodNotSupportedException

/**
 * GlobalExceptionHandler 가 정상적인 4xx 비즈니스 예외까지 ERROR 로 남기기 때문에,
 * 이 필터가 뚫리면 "출석 코드 불일치" 같은 로그가 디스코드 채널을 덮어 알림이 무의미해진다.
 */
class ErrorAlertFilterTest {
    private val filter =
        ErrorAlertFilter(
            excludedExceptions =
                "org.springframework.web.HttpRequestMethodNotSupportedException," +
                    "org.springframework.web.servlet.resource.NoResourceFoundException",
            excludedLoggers = "org.apache.noisy",
        )

    @Test
    fun `4xx 예외 코드가 담긴 메시지는 알리지 않는다`() {
        // GlobalExceptionHandler#handleBusinessException 이 남기는 형식
        val event = event("SESSION_NOT_FOUND Exception SESSION-404-01: 세션을 찾을 수 없습니다")

        assertThat(filter.shouldAlert(event)).isFalse()
    }

    @Test
    fun `일련번호가 한 자리인 코드도 4xx로 인식한다`() {
        val event = event("TOKEN_EXPIRED Exception JWT-401-2: 액세스 토큰이 만료되었습니다")

        assertThat(filter.shouldAlert(event)).isFalse()
    }

    @Test
    fun `도메인이 여러 단어인 코드도 4xx로 인식한다`() {
        val event = event("NOT_FOUND Exception ANNOUNCEMENT-READ-404-01: 없습니다")

        assertThat(filter.shouldAlert(event)).isFalse()
    }

    @Test
    fun `5xx 예외 코드는 알린다`() {
        val event = event("CREATE_FAILED Exception BILL-500-01: 정산 생성에 실패했습니다")

        assertThat(filter.shouldAlert(event)).isTrue()
    }

    @Test
    fun `예외 코드가 없는 예상치 못한 에러는 알린다`() {
        val event = event("Exception: NullPointerException - null", NullPointerException())

        assertThat(filter.shouldAlert(event)).isTrue()
    }

    @Test
    fun `필터 단에서 던져진 인증 예외는 알리지 않는다`() {
        // JwtAuthenticationFilter 의 예외는 advice 를 못 거쳐 톰캣이 대신 로깅한다. 메시지에 코드가 없다.
        val event =
            event(
                "Servlet.service() for servlet [dispatcherServlet] threw exception",
                InvalidAccessTokenException(JwtExceptionCode.AUTHORIZATION_HEADER_INVALID),
            )

        assertThat(filter.shouldAlert(event)).isFalse()
    }

    @Test
    fun `원인 예외로 감싸여 있어도 비즈니스 예외를 찾아낸다`() {
        val event =
            event(
                "Servlet.service() threw exception",
                RuntimeException("wrapped", InvalidAccessTokenException()),
            )

        assertThat(filter.shouldAlert(event)).isFalse()
    }

    @Test
    fun `5xx 비즈니스 예외는 예외 객체로 실려와도 알린다`() {
        val event = event("Servlet.service() threw exception", BusinessException(ServerSideCode))

        assertThat(filter.shouldAlert(event)).isTrue()
    }

    @Test
    fun `제외 목록에 있는 프레임워크 예외는 알리지 않는다`() {
        val event =
            event(
                "Exception: HttpRequestMethodNotSupportedException - Request method 'DELETE' is not supported",
                HttpRequestMethodNotSupportedException("DELETE"),
            )

        assertThat(filter.shouldAlert(event)).isFalse()
    }

    @Test
    fun `제외 목록에 있는 로거는 알리지 않는다`() {
        val event = event("무언가 터졌다", NullPointerException(), loggerName = "org.apache.noisy.Something")

        assertThat(filter.shouldAlert(event)).isFalse()
    }

    @Test
    fun `설정값에 공백이 섞여 있어도 예외 목록을 읽는다`() {
        // application.yml 은 여러 줄 문자열(>-)로 선언해 항목 사이에 공백이 들어간다
        val spaced =
            ErrorAlertFilter(
                excludedExceptions = " org.springframework.web.HttpRequestMethodNotSupportedException , java.lang.IllegalStateException ",
                excludedLoggers = "",
            )

        val event = event("터졌다", IllegalStateException("boom"))

        assertThat(spaced.shouldAlert(event)).isFalse()
    }

    @Test
    fun `WARN 이하 레벨은 알리지 않는다`() {
        val event = event("경고", level = Level.WARN)

        assertThat(filter.shouldAlert(event)).isFalse()
    }

    private fun event(
        message: String,
        throwable: Throwable? = null,
        loggerName: String = "core.application.common.exception.GlobalExceptionHandler",
        level: Level = Level.ERROR,
    ): LoggingEvent =
        LoggingEvent(
            loggerName,
            LoggerContext().getLogger(loggerName),
            level,
            message,
            throwable,
            emptyArray(),
        )

    private object ServerSideCode : ExceptionCode {
        override fun getStatus(): HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR

        override fun getCode(): String = "BILL-500-01"

        override fun getMessage(): String = "정산 생성에 실패했습니다"
    }
}

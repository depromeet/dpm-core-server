package core.application.common.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import core.application.common.exception.BusinessException

/**
 * ERROR 로그 중 Discord 로 보낼 것만 골라낸다.
 *
 * GlobalExceptionHandler 는 정상적인 4xx 비즈니스 예외까지 ERROR 로 남긴다.
 * 그대로 흘리면 "출석 코드 불일치", "토큰 만료" 같은 로그가 채널을 덮어 알림이 무의미해지므로,
 * 로그 레벨은 그대로 둔 채 여기서 걸러낸다.
 *
 * 상태 판단 근거가 두 갈래인 이유:
 * - 컨트롤러 예외는 advice 가 예외 객체 없이 메시지에만 코드를 남긴다 (`SESSION-404-01`).
 * - 필터 단 예외는 advice 를 거치지 못해 톰캣이 대신 로깅한다. 메시지에 코드가 없는 대신 예외 객체가 실려 있다.
 */
class ErrorAlertFilter(
    excludedExceptions: String,
    excludedLoggers: String,
) {
    private val excludedExceptionClasses: List<Class<*>> = loadClasses(excludedExceptions)
    private val excludedExceptionNames: List<String> = split(excludedExceptions)
    private val excludedLoggerPrefixes: List<String> = split(excludedLoggers)

    fun shouldAlert(event: ILoggingEvent): Boolean {
        if (event.level.toInt() < Level.ERROR_INT) return false
        if (excludedLoggerPrefixes.any { event.loggerName.startsWith(it) }) return false

        val chain = ThrowableProxies.chain(event.throwableProxy)
        if (chain.any(::isExcluded)) return false

        val status = businessStatusOf(chain) ?: statusFromMessage(event.formattedMessage)
        return status == null || status >= SERVER_ERROR_STATUS
    }

    private fun isExcluded(proxy: IThrowableProxy): Boolean {
        val throwable = ThrowableProxies.throwableOf(proxy)
        return if (throwable != null) {
            excludedExceptionClasses.any { it.isInstance(throwable) }
        } else {
            excludedExceptionNames.any { it == proxy.className }
        }
    }

    /** 비즈니스 예외가 실려 있으면 예외 코드가 들고 있는 HTTP 상태를 그대로 쓴다. */
    private fun businessStatusOf(chain: List<IThrowableProxy>): Int? =
        chain
            .asSequence()
            .mapNotNull { ThrowableProxies.throwableOf(it) as? BusinessException }
            .firstOrNull()
            ?.getCode()
            ?.getStatus()
            ?.value()

    /** `SESSION-404-01`, `JWT-401-1` 처럼 도메인-상태-일련번호 규칙을 따르는 예외 코드에서 상태를 뽑는다. */
    private fun statusFromMessage(message: String?): Int? {
        if (message.isNullOrBlank()) return null
        return EXCEPTION_CODE_PATTERN
            .find(message)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }

    private fun loadClasses(raw: String): List<Class<*>> =
        split(raw).mapNotNull { name ->
            runCatching { Class.forName(name, false, javaClass.classLoader) }.getOrNull()
        }

    private fun split(raw: String): List<String> =
        raw
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    companion object {
        private const val SERVER_ERROR_STATUS = 500
        private val EXCEPTION_CODE_PATTERN = Regex("[A-Z][A-Z_]*(?:-[A-Z_]+)*-(\\d{3})-\\d+")
    }
}

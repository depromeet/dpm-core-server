package core.application.common.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.InetAddress
import java.time.Instant

/**
 * 로그 이벤트를 Discord 웹훅 payload(JSON) 로 만든다.
 *
 * Discord 는 embed 하나의 description 4096자, 전체 6000자를 넘기면 400 을 돌려주므로 모든 필드를 잘라 넣는다.
 * 로그 메시지에는 토큰이나 이메일이 섞여 들어올 수 있어 전송 전에 마스킹한다.
 */
class DiscordMessageFormatter(
    private val appName: String,
    private val profile: String,
    private val stackTraceLines: Int,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    private val hostName: String = resolveHostName()

    fun format(
        event: ILoggingEvent,
        suppressedCount: Int,
    ): String {
        val chain = ThrowableProxies.chain(event.throwableProxy)
        val payload =
            mapOf(
                "username" to "$appName [$profile]",
                "embeds" to
                    listOf(
                        mapOf(
                            "title" to title(chain),
                            "description" to description(event, chain),
                            "color" to ERROR_COLOR,
                            "timestamp" to Instant.ofEpochMilli(event.timeStamp).toString(),
                            "fields" to fields(event, suppressedCount),
                        ),
                    ),
            )
        return objectMapper.writeValueAsString(payload)
    }

    private fun title(chain: List<IThrowableProxy>): String {
        val exceptionName = chain.firstOrNull()?.className?.substringAfterLast('.')
        return truncate("🚨 [$profile] ${exceptionName ?: "Error"}", TITLE_LIMIT)
    }

    private fun description(
        event: ILoggingEvent,
        chain: List<IThrowableProxy>,
    ): String {
        val message = truncate(mask(event.formattedMessage.orEmpty()), MESSAGE_LIMIT)
        if (chain.isEmpty()) {
            return codeBlock(message)
        }
        val remaining = DESCRIPTION_LIMIT - message.length - CODE_BLOCK_OVERHEAD
        val stackTrace = truncate(mask(renderStackTrace(chain)), remaining.coerceAtLeast(0))
        return codeBlock(message) + "\n" + codeBlock(stackTrace)
    }

    private fun fields(
        event: ILoggingEvent,
        suppressedCount: Int,
    ): List<Map<String, Any>> {
        // 알림 조립이 실패해 로그가 통째로 사라지면 안 되므로, 맥락 정보는 없으면 없는 대로 진행한다.
        val mdc = runCatching { event.mdcPropertyMap }.getOrNull().orEmpty()
        val request =
            listOfNotNull(mdc[MdcLoggingFilter.HTTP_METHOD], mdc[MdcLoggingFilter.REQUEST_URI])
                .joinToString(" ")

        return buildList {
            add(field("환경", "$profile / $hostName"))
            add(field("로거", event.loggerName.substringAfterLast('.')))
            add(field("스레드", event.threadName))
            if (request.isNotBlank()) {
                add(field("요청", request, inline = false))
            }
            mdc[MdcLoggingFilter.REQUEST_ID]?.let { add(field("requestId", it)) }
            mdc[MdcLoggingFilter.CLIENT_IP]?.let { add(field("clientIp", it)) }
            if (suppressedCount > 0) {
                add(field("생략된 동일 알림", "${suppressedCount}건", inline = false))
            }
        }
    }

    private fun field(
        name: String,
        value: String,
        inline: Boolean = true,
    ): Map<String, Any> =
        mapOf(
            "name" to name,
            "value" to truncate(mask(value), FIELD_VALUE_LIMIT),
            "inline" to inline,
        )

    private fun renderStackTrace(chain: List<IThrowableProxy>): String =
        buildString {
            chain.forEachIndexed { index, proxy ->
                if (index > 0) append("Caused by: ")
                append(proxy.className)
                proxy.message?.let { append(": ").append(it) }
                append("\n")

                val steps = proxy.stackTraceElementProxyArray.orEmpty()
                steps.take(stackTraceLines).forEach { append("\tat ").append(it.steAsString).append("\n") }
                if (steps.size > stackTraceLines) {
                    append("\t... ").append(steps.size - stackTraceLines).append(" more\n")
                }
            }
        }.trimEnd()

    private fun mask(text: String): String =
        MASK_PATTERNS.fold(text) { masked, (pattern, replacement) ->
            pattern.replace(masked, replacement)
        }

    private fun codeBlock(text: String): String = "```\n$text\n```"

    private fun truncate(
        text: String,
        limit: Int,
    ): String =
        when {
            limit <= 0 -> ""
            text.length <= limit -> text
            limit <= ELLIPSIS.length -> text.take(limit)
            else -> text.take(limit - ELLIPSIS.length) + ELLIPSIS
        }

    /**
     * 컨테이너가 주입한 값을 먼저 본다.
     * [InetAddress.getLocalHost] 는 환경에 따라 역방향 DNS 조회로 넘어가 수 초간 멈출 수 있는데,
     * 이 함수는 로깅 초기화 시점, 즉 애플리케이션 기동 경로에서 호출된다.
     */
    private fun resolveHostName(): String =
        System.getenv("HOSTNAME")?.takeIf { it.isNotBlank() }
            ?: runCatching { InetAddress.getLocalHost().hostName }.getOrNull()
            ?: "unknown"

    companion object {
        private const val ERROR_COLOR = 0xE74C3C
        private const val TITLE_LIMIT = 256
        private const val FIELD_VALUE_LIMIT = 1024
        private const val DESCRIPTION_LIMIT = 3800
        private const val MESSAGE_LIMIT = 800
        private const val CODE_BLOCK_OVERHEAD = 16
        private const val ELLIPSIS = "…(생략)"

        private val MASK_PATTERNS: List<Pair<Regex, String>> =
            listOf(
                Regex("(?i)bearer\\s+[A-Za-z0-9\\-._~+/]+=*") to "Bearer ***",
                Regex("eyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]*") to "***.jwt.***",
                Regex("(?i)(password|secret|token|authorization)([\"']?\\s*[=:]\\s*)\\S+") to "$1$2***",
                // OAuth 콜백 URI 는 MDC 를 타고 알림에 그대로 실린다. 일회성이라도 인증 크리덴셜이다.
                Regex("(?i)([?&](?:code|state|id_token|access_token|refresh_token|client_secret)=)[^&\\s]+") to "$1***",
                Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}") to "***@***",
            )
    }
}

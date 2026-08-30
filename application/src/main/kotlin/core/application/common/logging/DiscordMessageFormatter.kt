package core.application.common.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.StackTraceElementProxy
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.InetAddress
import java.time.Instant

/**
 * 로그 이벤트를 Discord 웹훅 payload(JSON) 로 만든다.
 *
 * Discord 는 embed 하나의 description 4096자, 전체 6000자를 넘기면 400 을 돌려주므로 모든 필드를 잘라 넣는다.
 * 로그 메시지에는 토큰이나 이메일이 섞여 들어올 수 있어 전송 전에 마스킹한다.
 *
 * 본문은 "무엇이 터졌나(원인 체인) / 어디서 터졌나(우리 코드 프레임)" 두 단으로 나눈다.
 * 예외 전문을 그대로 실으면 reflection, 스프링, 톰캣 프레임이 화면을 덮어 정작 볼 것이 묻힌다.
 * 알림은 "이거 봐야 하나" 를 판단하는 용도고, 전문이 필요하면 콘솔 로그를 본다.
 */
class DiscordMessageFormatter(
    private val appName: String,
    private val profile: String,
    private val stackTraceLines: Int,
    includePackages: String,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) {
    private val hostName: String = resolveHostName()

    /** 스택트레이스에서 남길 프레임의 패키지 접두사. 비어 있으면 접지 않고 상위 몇 줄만 보여준다. */
    private val includedPackages: List<String> =
        includePackages
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun format(
        event: ILoggingEvent,
        suppressedCount: Int,
    ): String {
        val chain = ThrowableProxies.chain(event.throwableProxy)
        val payload =
            mapOf(
                "username" to "$appName [$profile]",
                // 로그 메시지에 섞여 들어온 "@everyone" 이 멘션으로 살아나지 않게 원천 차단한다.
                "allowed_mentions" to mapOf("parse" to emptyList<String>()),
                "embeds" to
                    listOf(
                        mapOf(
                            "title" to title(chain),
                            "description" to description(event, chain),
                            "color" to ERROR_COLOR,
                            "timestamp" to Instant.ofEpochMilli(event.timeStamp).toString(),
                            "fields" to fields(event),
                            "footer" to mapOf("text" to footer(event, suppressedCount)),
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
        val message = truncate(escapeMarkdown(mask(event.formattedMessage.orEmpty())), MESSAGE_LIMIT)
        if (chain.isEmpty()) {
            return message
        }

        val body =
            buildString {
                append(message)
                // 예외가 하나뿐이면 제목이 이미 이름을 보여주므로 소제목까지 붙이지 않는다.
                // 블록 자체는 항상 넣는다. 로그 메시지가 밋밋할 때 단서는 예외 메시지 쪽에 있다.
                append(if (chain.size > 1) "\n\n**원인 체인**\n" else "\n\n")
                append(codeBlock(mask(renderCauseChain(chain))))
                append("\n")
                append(renderFrameSection(chain))
            }
        return truncate(body, DESCRIPTION_LIMIT)
    }

    /**
     * 감싼 예외부터 마지막 원인까지 한 줄씩 들여쓴다.
     *
     * `IllegalStateException` 처럼 감싼 예외 이름만으로는 무슨 일인지 알 수 없는 경우가 많은데,
     * 실제 단서는 맨 아래 원인에 있다. 스택과 섞어두면 그 한 줄을 찾느라 전문을 훑어야 한다.
     */
    private fun renderCauseChain(chain: List<IThrowableProxy>): String =
        chain
            .mapIndexed { index, proxy ->
                val indent = if (index == 0) "" else CAUSE_INDENT.repeat(index - 1) + CAUSE_MARKER
                val name = proxy.className.substringAfterLast('.')
                val message = proxy.message?.let { ": $it" }.orEmpty()
                indent + truncate(name + message, CAUSE_LINE_LIMIT)
            }.joinToString("\n")

    private fun renderFrameSection(chain: List<IThrowableProxy>): String {
        val ours = ourFrames(chain)
        if (ours.isEmpty()) {
            // 프레임워크 안에서만 터진 예외는 접을 것이 없다. 이때는 패키지까지 보여야 어디인지 알아볼 수 있다.
            val head = chain.first().stackTraceElementProxyArray.orEmpty().take(FALLBACK_FRAMES)
            if (head.isEmpty()) return ""
            return "**스택 상위 ${head.size}줄**\n" +
                codeBlock(mask(head.joinToString("\n") { it.steAsString }))
        }

        val kept = ours.take(stackTraceLines)
        val hidden = totalFrames(chain) - kept.size
        val header = if (hidden > 0) "**우리 코드 프레임** · 나머지 ${hidden}줄 생략" else "**우리 코드 프레임**"
        return header + "\n" + codeBlock(mask(kept.joinToString("\n", transform = ::compact)))
    }

    /**
     * 체인 전체에서 우리 코드 프레임만 순서대로 모은다.
     *
     * 원인 예외의 스택은 감싼 예외와 아래쪽 프레임을 그대로 공유한다. (Java 가 `... 52 more` 로 줄이는 부분)
     * 겹치는 꼬리를 잘라내지 않으면 같은 프레임이 예외 수만큼 반복되어 나온다.
     */
    private fun ourFrames(chain: List<IThrowableProxy>): List<StackTraceElementProxy> =
        chain.flatMapIndexed { index, proxy ->
            distinctFrames(proxy, index).filter(::isOurs)
        }

    private fun totalFrames(chain: List<IThrowableProxy>): Int =
        chain
            .mapIndexed { index, proxy -> distinctFrames(proxy, index).size }
            .sum()

    private fun distinctFrames(
        proxy: IThrowableProxy,
        index: Int,
    ): List<StackTraceElementProxy> {
        val frames = proxy.stackTraceElementProxyArray.orEmpty().asList()
        return if (index == 0) frames else frames.dropLast(proxy.commonFrames.coerceIn(0, frames.size))
    }

    private fun isOurs(frame: StackTraceElementProxy): Boolean =
        includedPackages.any { frame.stackTraceElement.className.startsWith(it) }

    /**
     * `core.application.attendance...AttendanceCommandService.checkIn(AttendanceCommandService.kt:88)`
     * 를 `AttendanceCommandService.checkIn:88` 로 줄인다.
     *
     * 임베드 폭이 좁아 긴 프레임은 두세 줄로 접히는데, 그러면 프레임 경계가 사라져 오히려 읽기 어려워진다.
     * 클래스명과 줄 번호만 있으면 IDE 에서 바로 찾아갈 수 있으므로 패키지와 파일명은 버린다.
     */
    private fun compact(frame: StackTraceElementProxy): String {
        val element = frame.stackTraceElement
        val className =
            element.className
                .substringAfterLast('.')
                .substringBefore('$')
        val line = if (element.lineNumber >= 0) ":${element.lineNumber}" else ""
        return "$className.${element.methodName}$line"
    }

    private fun fields(event: ILoggingEvent): List<Map<String, Any>> {
        val mdc = mdcOf(event)
        val request =
            listOfNotNull(mdc[MdcLoggingFilter.HTTP_METHOD], mdc[MdcLoggingFilter.REQUEST_URI])
                .joinToString(" ")

        return buildList {
            if (request.isNotBlank()) {
                add(field("요청", request, inline = false))
            }
            // 값이 비면 이름만 덩그러니 남은 칸이 생긴다. Discord 는 막지 않으므로 여기서 거른다.
            mdc[MdcLoggingFilter.MEMBER_ID]?.takeIf { it.isNotBlank() }?.let { add(field("사용자", "memberId $it")) }
            mdc[MdcLoggingFilter.REQUEST_ID]?.takeIf { it.isNotBlank() }?.let { add(field("requestId", it)) }
        }
    }

    /** 매번 눈으로 좇을 필요는 없지만 없으면 곤란한 값들. 작은 회색 글씨로 한 줄에 붙인다. */
    private fun footer(
        event: ILoggingEvent,
        suppressedCount: Int,
    ): String {
        val parts =
            listOfNotNull(
                "$profile / $hostName",
                event.loggerName.substringAfterLast('.'),
                event.threadName,
                if (suppressedCount > 0) "동일 알림 ${suppressedCount}건 생략" else null,
            )
        return truncate(mask(parts.joinToString(" · ")), FOOTER_LIMIT)
    }

    /** 알림 조립이 실패해 로그가 통째로 사라지면 안 되므로, 맥락 정보는 없으면 없는 대로 진행한다. */
    private fun mdcOf(event: ILoggingEvent): Map<String, String> =
        runCatching { event.mdcPropertyMap }.getOrNull().orEmpty()

    private fun field(
        name: String,
        value: String,
        inline: Boolean = true,
    ): Map<String, Any> =
        mapOf(
            "name" to name,
            "value" to truncate(escapeMarkdown(mask(value)), FIELD_VALUE_LIMIT),
            "inline" to inline,
        )

    private fun mask(text: String): String =
        MASK_PATTERNS.fold(text) { masked, (pattern, replacement) ->
            pattern.replace(masked, replacement)
        }

    /**
     * 로그에 ``` 가 섞여 들어오면 코드블록이 그 자리에서 닫히고 이후 본문이 통째로 깨진다.
     * 폭 0 공백을 끼워 펜스로 인식되지 않게 한다.
     */
    private fun codeBlock(text: String): String = "```java\n${text.replace("```", "`" + ZERO_WIDTH_SPACE + "``")}\n```"

    /**
     * 임베드의 본문과 필드 값은 마크다운으로 해석된다. 코드블록 밖에 놓이는 값은 원문 그대로 보이게 막아야 한다.
     * `member_id_value` 가 기울임이 되거나 줄머리 `#` 가 헤딩으로 바뀌면 정작 봐야 할 로그를 못 읽는다.
     */
    private fun escapeMarkdown(text: String): String {
        val inlineEscaped = INLINE_MARKDOWN.replace(text) { "\\${it.value}" }
        return LINE_LEADING_MARKDOWN.replace(inlineEscaped) { "\\${it.value}" }
    }

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
        private const val FOOTER_LIMIT = 2048
        private const val DESCRIPTION_LIMIT = 3800
        private const val MESSAGE_LIMIT = 800
        private const val CAUSE_LINE_LIMIT = 200
        private const val CAUSE_INDENT = "   "
        private const val CAUSE_MARKER = "└─ "
        private const val FALLBACK_FRAMES = 3
        private const val ZERO_WIDTH_SPACE = "\u200b"

        /** 어디에 있든 서식이 되는 문자들. 백슬래시도 함께 잡아야 우리가 붙인 이스케이프가 다시 해석되지 않는다. */
        private val INLINE_MARKDOWN = Regex("""[\\`*_~|\[\]]""")

        /** 줄머리에서만 서식이 되는 문자들. 헤딩, 목록, 인용. */
        private val LINE_LEADING_MARKDOWN = Regex("""(?m)^[>#-]""")
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

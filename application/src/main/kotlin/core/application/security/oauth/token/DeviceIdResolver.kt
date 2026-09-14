package core.application.security.oauth.token

import core.application.security.properties.SecurityProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

/**
 * 기기 식별자를 확보한다. 우선순위는 헤더 → 쿠키 → 신규 발급.
 *
 * 앱이 WebView 껍데기라 웹 JS 는 기기 고유값을 모른다. 서버가 쿠키로 발급하면
 * 앱 배포도 FE 변경도 필요 없다. 쿠키가 지워지면 새 기기로 인식되지만
 * 회원당 토큰 상한이 흡수하므로 best-effort 로 충분하다.
 *
 * 헤더 분기는 향후 앱 브릿지가 getDeviceId() 를 내려주면 그대로 우선 채택된다.
 */
@Component
class DeviceIdResolver(
    private val securityProperties: SecurityProperties,
) {
    fun resolve(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        peek(request)?.let { return it }

        val issued = UUID.randomUUID().toString()
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(issued))
        return issued
    }

    /** 응답에 쿠키를 심지 않고 조회만 한다. 로그아웃 · 진단 로그용. */
    fun peek(request: HttpServletRequest): String? =
        request
            .getHeader(DEVICE_ID_HEADER)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.take(MAX_LENGTH)
            ?: request.cookies
                ?.lastOrNull { it.name == DEVICE_ID_COOKIE }
                ?.value
                ?.takeIf { it.isNotBlank() }
                ?.take(MAX_LENGTH)

    private fun buildCookie(deviceId: String): String {
        val builder =
            ResponseCookie
                .from(DEVICE_ID_COOKIE, deviceId)
                .path("/")
                .maxAge(Duration.ofDays(DEVICE_COOKIE_DAYS))
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE_NONE)

        resolveCookieDomain()?.let(builder::domain)

        return builder.build().toString()
    }

    private fun resolveCookieDomain(): String? {
        val configuredDomain = securityProperties.cookie.domain.trim()
        if (configuredDomain.isBlank() || configuredDomain.equals("localhost", ignoreCase = true)) {
            return null
        }

        return configuredDomain.removePrefix(".")
    }

    companion object {
        const val DEVICE_ID_COOKIE = "__dpm_did"
        const val DEVICE_ID_HEADER = "X-Device-Id"
        private const val MAX_LENGTH = 128
        private const val DEVICE_COOKIE_DAYS = 400L
        private const val SAME_SITE_NONE = "None"
    }
}

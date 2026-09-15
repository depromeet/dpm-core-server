package core.application.security.oauth.redirect

import core.application.security.properties.SecurityProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64

@Component
class OAuthCallbackRedirectService(
    private val redirectUriValidator: OAuthRedirectUriValidator,
    private val securityProperties: SecurityProperties,
) {
    fun rememberClientRedirectUri(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val redirectUri = resolveClientRedirectUri(request) ?: return

        response.addHeader(
            SET_COOKIE_HEADER,
            buildCookie(
                name = REDIRECT_COOKIE_NAME,
                value = encode(redirectUri),
                maxAgeSeconds = REDIRECT_COOKIE_MAX_AGE_SECONDS,
            ),
        )
    }

    fun buildSuccessRedirectUri(request: HttpServletRequest): String =
        buildRedirectUri(
            baseRedirectUri = loadClientRedirectUri(request) ?: buildDefaultRedirectUri(request),
            authenticated = true,
            errorCode = null,
        )

    fun buildFailureRedirectUri(
        request: HttpServletRequest,
        errorCode: String,
    ): String =
        buildRedirectUri(
            baseRedirectUri = loadClientRedirectUri(request) ?: buildDefaultRedirectUri(request),
            authenticated = false,
            errorCode = errorCode,
        )

    fun clearClientRedirectUri(response: HttpServletResponse) {
        response.addHeader(
            SET_COOKIE_HEADER,
            buildCookie(
                name = REDIRECT_COOKIE_NAME,
                value = "",
                maxAgeSeconds = 0,
            ),
        )
    }

    private fun resolveClientRedirectUri(request: HttpServletRequest): String? =
        listOfNotNull(request.getHeader(REFERER), request.getHeader(ORIGIN))
            .firstNotNullOfOrNull(::toAppOriginRedirect)

    private fun loadClientRedirectUri(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == REDIRECT_COOKIE_NAME }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?.let(::decode)
            ?.let(::toAppOriginRedirect)

    /**
     * Remember only the FE origin root ("https://host:port/").
     * Using the full Referer would bounce users back to /login after Kakao OAuth.
     */
    private fun toAppOriginRedirect(candidate: String): String? =
        runCatching {
            val validatedUri = redirectUriValidator.validate(candidate)
            val uri = URI.create(validatedUri)
            val host = uri.host.lowercase()

            require(host !in DISALLOWED_REDIRECT_HOSTS && DISALLOWED_REDIRECT_PREFIXES.none(host::startsWith)) {
                "Disallowed OAuth browser redirect target: $host"
            }

            URI(uri.scheme, uri.authority, "/", null, null).toString()
        }.getOrNull()

    private fun buildRedirectUri(
        baseRedirectUri: String,
        authenticated: Boolean,
        errorCode: String?,
    ): String {
        val builder =
            UriComponentsBuilder
                .fromUriString(baseRedirectUri)
                .replaceQueryParam(AUTHENTICATED_PARAM)
                .replaceQueryParam(ERROR_CODE_PARAM)
                .queryParam(AUTHENTICATED_PARAM, authenticated)

        if (errorCode != null) {
            builder.queryParam(ERROR_CODE_PARAM, errorCode)
        }

        return builder
            .build()
            .encode()
            .toUriString()
    }

    private fun buildDefaultRedirectUri(request: HttpServletRequest): String {
        val requestHost = request.serverName.lowercase()

        return when {
            requestHost == "localhost" || requestHost == "127.0.0.1" || requestHost == "::1" ->
                // Prefer remembered FE origin cookie; this is only a last-resort fallback.
                "https://local-admin.depromeet.shop:3020/"
            requestHost.startsWith("api.") ->
                "https://${requestHost.replaceFirst("api.", "core.")}/"
            requestHost.startsWith("local-api.") ->
                "https://${requestHost.replaceFirst("local-api.", "local-core.")}/"
            requestHost.startsWith("dev-api.") ->
                "https://${requestHost.replaceFirst("dev-api.", "dev-core.")}/"
            else -> {
                val portSuffix =
                    when {
                        request.serverPort <= 0 -> ""
                        request.scheme == "http" && request.serverPort == 80 -> ""
                        request.scheme == "https" && request.serverPort == 443 -> ""
                        else -> ":${request.serverPort}"
                    }
                "${request.scheme}://$requestHost$portSuffix/"
            }
        }
    }

    private fun buildCookie(
        name: String,
        value: String,
        maxAgeSeconds: Long,
    ): String {
        // SameSite=None requires Secure. On local HTTP (secure=false) use Lax so the
        // browser actually stores OAUTH2_REDIRECT_URI and returns to the right FE.
        val secure = securityProperties.cookie.secure
        val sameSite = if (secure) SAME_SITE_NONE else SAME_SITE_LAX
        val builder =
            ResponseCookie
                .from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))

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

    private fun encode(value: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decode(value: String): String = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    companion object {
        private const val SET_COOKIE_HEADER = "Set-Cookie"
        private const val REDIRECT_COOKIE_NAME = "OAUTH2_REDIRECT_URI"
        private const val REDIRECT_COOKIE_MAX_AGE_SECONDS = 180L
        private const val REFERER = "Referer"
        private const val ORIGIN = "Origin"
        private const val SAME_SITE_NONE = "None"
        private const val SAME_SITE_LAX = "Lax"
        private const val AUTHENTICATED_PARAM = "authenticated"
        private const val ERROR_CODE_PARAM = "errorCode"

        private val DISALLOWED_REDIRECT_HOSTS =
            setOf(
                "appleid.apple.com",
                "kauth.kakao.com",
            )

        private val DISALLOWED_REDIRECT_PREFIXES =
            setOf(
                "api.",
                "local-api.",
            )
    }
}

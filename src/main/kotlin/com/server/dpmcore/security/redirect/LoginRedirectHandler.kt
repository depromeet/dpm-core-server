package com.server.dpmcore.security.redirect

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.common.constant.Profile
import com.server.dpmcore.security.properties.SecurityProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

private const val REQUEST_DOMAIN = "REQUEST_DOMAIN"

@Component
class LoginRedirectHandler(
    private val environment: Environment,
    private val securityProperties: SecurityProperties,
) {
    private val rules: List<RedirectRule> =
        listOf(
            // LOCAL
            RedirectRule(
                AuthorityType.DEEPER,
                Profile.LOCAL,
                "localhost:8080",
                "https://localhost:8080/swagger-ui/index.html",
            ),
            RedirectRule(
                AuthorityType.ORGANIZER,
                Profile.LOCAL,
                "localhost:8080",
                "https://localhost:8080/swagger-ui/index.html",
            ),
            RedirectRule(
                AuthorityType.DEEPER,
                Profile.LOCAL,
                "local-core.depromeet-core.shop:3010",
                "https://local-core.depromeet-core.shop:3010?isAdmin=false",
            ),
            RedirectRule(
                AuthorityType.DEEPER,
                Profile.LOCAL,
                "local-admin.depromeet-core.shop:3010",
                "https://local-core.depromeet-core.shop:3010?isAdmin=false",
            ),
            RedirectRule(
                AuthorityType.ORGANIZER,
                Profile.LOCAL,
                "local-core.depromeet-core.shop:3010",
                "https://local-core.depromeet-core.shop:3010?isAdmin=true",
            ),
            RedirectRule(
                AuthorityType.ORGANIZER,
                Profile.LOCAL,
                "local-admin.depromeet-core.shop:3020",
                "https://local-admin.depromeet-core.shop:3020?isAdmin=true",
            ),
            // DEV
            RedirectRule(
                AuthorityType.DEEPER,
                Profile.DEV,
                "client.depromeet-core.shop",
                "https://client.depromeet-core.shop?isAdmin=false",
            ),
            RedirectRule(
                AuthorityType.DEEPER,
                Profile.DEV,
                "admin.depromeet-core.shop",
                "https://client.depromeet-core.shop?isAdmin=false",
            ),
            RedirectRule(
                AuthorityType.ORGANIZER,
                Profile.DEV,
                "client.depromeet-core.shop",
                "https://client.depromeet-core.shop?isAdmin=true",
            ),
            RedirectRule(
                AuthorityType.ORGANIZER,
                Profile.DEV,
                "admin.depromeet-core.shop",
                "https://admin.depromeet-core.shop?isAdmin=true",
            ),
            // PROD
            RedirectRule(
                AuthorityType.DEEPER,
                Profile.PROD,
                "core.depromeet.com",
                "https://core.depromeet.com?isAdmin=false",
            ),
            RedirectRule(
                AuthorityType.DEEPER,
                Profile.PROD,
                "admin.depromeet.com",
                "https://core.depromeet.com?isAdmin=false",
            ),
            RedirectRule(
                AuthorityType.ORGANIZER,
                Profile.PROD,
                "core.depromeet.com",
                "https://core.depromeet.com?isAdmin=true",
            ),
            RedirectRule(
                AuthorityType.ORGANIZER,
                Profile.PROD,
                "admin.depromeet.com",
                "https://admin.depromeet.com?isAdmin=true",
            ),
        )

    fun determineRedirectUrl(
        authority: AuthorityType,
        request: HttpServletRequest,
    ): String {
        val matchedRule =
            rules.find { rule ->
                rule.authority == authority &&
                    rule.requestURL == extractRequestURL(request) &&
                    rule.profile == Profile.get(environment)
            }
        return matchedRule?.target ?: securityProperties.restrictedRedirectUrl
    }

    private fun extractRequestURL(request: HttpServletRequest): String {
        return request.cookies?.firstOrNull { it.name == REQUEST_DOMAIN }?.value ?: request.serverName
    }
}

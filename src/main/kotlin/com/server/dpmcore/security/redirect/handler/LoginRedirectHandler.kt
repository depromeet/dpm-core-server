package com.server.dpmcore.security.redirect.handler

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.common.constant.Profile
import com.server.dpmcore.security.redirect.model.LoginIntent
import com.server.dpmcore.security.redirect.model.RedirectContext
import com.server.dpmcore.security.redirect.strategy.CompositeRedirectStrategy
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

private const val REQUEST_DOMAIN = "REQUEST_DOMAIN"

@Component
class LoginRedirectHandler(
    private val strategy: CompositeRedirectStrategy,
) {
    fun determineRedirectUrl(
        request: HttpServletRequest,
        authority: AuthorityType,
        profile: Profile,
    ): String {
        val context =
            RedirectContext(
                authority = authority,
                profile = profile,
                requestUrl = extractRequestURL(request),
                intent = LoginIntent.DIRECT,
            )

        return strategy.resolve(context)
    }

    private fun extractRequestURL(request: HttpServletRequest): String {
        return request.cookies?.firstOrNull { it.name == REQUEST_DOMAIN }?.value ?: request.serverName
    }
}

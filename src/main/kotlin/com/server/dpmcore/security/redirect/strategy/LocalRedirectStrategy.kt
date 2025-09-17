package com.server.dpmcore.security.redirect.strategy

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.common.constant.Profile
import com.server.dpmcore.security.properties.SecurityProperties
import com.server.dpmcore.security.redirect.model.RedirectContext
import com.server.dpmcore.security.redirect.validator.RedirectValidator

class LocalRedirectStrategy(
    private val properties: SecurityProperties,
    private val validator: RedirectValidator,
    private val errorStrategy: ErrorRedirectStrategy,
) : RedirectStrategy {
    override fun supports(context: RedirectContext): Boolean {
        val url = context.requestUrl ?: return false
        return context.profile == Profile.DEV && listOf("local-core", "local-admin").any { url.contains(it) }
    }

    override fun resolve(context: RedirectContext): String {
        val requestUrl =
            context.requestUrl ?: context.copy(error = "Request URL is missing").let {
                return errorStrategy.resolve(it)
            }

        val redirectUrl =
            when (context.authority) {
                AuthorityType.DEEPER -> resolveDeeperRedirect(requestUrl)
                AuthorityType.ORGANIZER -> resolveOrganizerRedirect(requestUrl)
                else -> null
            } ?: context.copy(error = "Unsupported Redirect URL").let { errorStrategy.resolve(it) }

        return validator.validate(redirectUrl)
    }

    private fun resolveDeeperRedirect(requestUrl: String): String? =
        when {
            requestUrl.startsWith("local-core.") -> "https://local-core.${properties.cookie.domain}:3010?isAdmin=false"
            requestUrl.startsWith("local-admin.") -> "https://local-core.${properties.cookie.domain}:3010?isAdmin=false"
            else -> null
        }

    private fun resolveOrganizerRedirect(requestUrl: String): String? =
        when {
            requestUrl.startsWith("local-core.") -> "https://local-core.${properties.cookie.domain}:3010?isAdmin=true"
            requestUrl.startsWith("local-admin.") -> "https://local-admin.${properties.cookie.domain}:3020?isAdmin=true"
            else -> null
        }
}

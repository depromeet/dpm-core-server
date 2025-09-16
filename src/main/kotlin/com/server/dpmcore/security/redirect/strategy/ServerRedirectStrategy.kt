package com.server.dpmcore.security.redirect.strategy

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.common.constant.Profile
import com.server.dpmcore.security.properties.SecurityProperties
import com.server.dpmcore.security.redirect.model.RedirectContext
import com.server.dpmcore.security.redirect.validator.RedirectValidator

class ServerRedirectStrategy(
    private val properties: SecurityProperties,
    private val validator: RedirectValidator,
    private val errorStrategy: ErrorRedirectStrategy,
) : RedirectStrategy {
    override fun supports(context: RedirectContext): Boolean = context.profile in listOf(Profile.DEV, Profile.PROD)

    override fun resolve(context: RedirectContext): String {
        val requestUrl =
            context.requestUrl ?: context.copy(error = "Request URL is missing").let {
                return errorStrategy.resolve(it)
            }

        val profile =
            context.profile ?: context.copy(error = "Invalid Server Environment").let {
                return errorStrategy.resolve(it)
            }

        val redirectUrl =
            when (context.authority) {
                AuthorityType.DEEPER -> resolveDeeperRedirect(requestUrl, profile)
                AuthorityType.ORGANIZER -> resolveOrganizerRedirect(requestUrl, profile)
                else -> null
            } ?: context.copy(error = "Unsupported Redirect URL").let { errorStrategy.resolve(it) }

        return validator.validate(redirectUrl)
    }

    private fun resolveDeeperRedirect(
        requestUrl: String,
        profile: Profile,
    ): String? =
        when {
            requestUrl.contains("client") && profile == Profile.DEV ->
                "${properties.redirect.coreRedirectUrl}?isAdmin=false"

            requestUrl.contains("core") && profile == Profile.PROD ->
                "${properties.redirect.coreRedirectUrl}?isAdmin=false"

            requestUrl.contains("admin") -> "${properties.redirect.coreRedirectUrl}?isAdmin=false"
            else -> null
        }

    private fun resolveOrganizerRedirect(
        requestUrl: String,
        profile: Profile,
    ): String? =
        when {
            requestUrl.contains("client") && profile == Profile.DEV ->
                "${properties.redirect.coreRedirectUrl}?isAdmin=true"

            requestUrl.contains("core") && profile == Profile.PROD ->
                "${properties.redirect.coreRedirectUrl}?isAdmin=true"

            requestUrl.contains("admin") -> "${properties.redirect.adminRedirectUrl}?isAdmin=true"
            else -> null
        }
}

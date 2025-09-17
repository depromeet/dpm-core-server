package com.server.dpmcore.security.redirect.strategy

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.common.constant.Profile
import com.server.dpmcore.security.properties.SecurityProperties
import com.server.dpmcore.security.redirect.model.RedirectContext
import com.server.dpmcore.security.redirect.validator.RedirectValidator

/**
 * `DEV` 환경에서 프론트엔드가 로컬(`local-core`, `local-admin`)에서 구동될 때 사용하는 리다이렉트 전략입니다.
 *
 * 프론트엔드 로컬에서 구동되는 도메인은 아래와 같습니다.
 * - client: `local-core.depromeet-core.shop:3010`
 * - admin: `local-admin.depromeet-core.shop:3020`
 *
 * @author LeeHanEum
 * @since 2025.09.17
 */
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

package core.application.security.redirect.strategy

import core.application.common.constant.Profile
import core.application.security.properties.SecurityProperties
import core.application.security.redirect.model.RedirectContext
import core.application.security.redirect.validator.RedirectValidator
import core.domain.authority.enums.AuthorityType

/**
 * `PROD` 및 `DEV` 환경에서의 리다이렉트 URL을 결정하는 전략 클래스입니다.
 *
 * @author LeeHanEum
 * @since 2025.09.17
 */
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
            requestUrl.startsWith("core.") -> "${properties.redirect.coreRedirectUrl}?isAdmin=false"
            requestUrl.startsWith("admin.") -> "${properties.redirect.coreRedirectUrl}?isAdmin=false"
            else -> null
        }

    private fun resolveOrganizerRedirect(requestUrl: String): String? =
        when {
            requestUrl.startsWith("core.") -> "${properties.redirect.coreRedirectUrl}?isAdmin=true"
            requestUrl.startsWith("admin.") -> "${properties.redirect.adminRedirectUrl}?isAdmin=true"
            else -> null
        }
}

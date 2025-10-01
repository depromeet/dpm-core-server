package core.application.security.redirect.handler

import core.application.security.redirect.model.LoginIntent
import core.application.security.redirect.model.RedirectContext
import core.application.security.redirect.strategy.CompositeRedirectStrategy
import core.application.common.constant.Profile
import core.domain.authority.enums.AuthorityType
import org.springframework.stereotype.Component

@Component
class LoginRedirectHandler(
    private val strategy: CompositeRedirectStrategy,
) {
    fun determineRedirectUrl(
        requestUrl: String,
        authority: AuthorityType,
        profile: Profile,
    ): String {
        val context =
            RedirectContext(
                authority = authority,
                profile = profile,
                requestUrl = requestUrl,
                intent = LoginIntent.DIRECT,
            )

        return strategy.resolve(context)
    }
}

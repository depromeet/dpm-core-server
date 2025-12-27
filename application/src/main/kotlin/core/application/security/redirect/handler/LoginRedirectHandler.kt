package core.application.security.redirect.handler

import core.application.common.constant.Profile
import core.application.security.redirect.model.LoginIntent
import core.application.security.redirect.model.RedirectContext
import core.application.security.redirect.strategy.CompositeRedirectStrategy
import core.domain.authorization.vo.RoleType
import org.springframework.stereotype.Component

@Component
class LoginRedirectHandler(
    private val strategy: CompositeRedirectStrategy,
) {
    fun determineRedirectUrl(
        requestUrl: String,
        role: RoleType,
        profile: Profile,
    ): String {
        val context =
            RedirectContext(
                role = role,
                profile = profile,
                requestUrl = requestUrl,
                intent = LoginIntent.DIRECT,
            )

        println(
            "LoginRedirectHandler.determineRedirectUrl: role=$role, profile=$profile, requestUrl=$requestUrl",
        )

        return strategy.resolve(context)
    }
}

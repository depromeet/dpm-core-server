package com.server.dpmcore.security.redirect.handler

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.common.constant.Profile
import com.server.dpmcore.security.redirect.model.LoginIntent
import com.server.dpmcore.security.redirect.model.RedirectContext
import com.server.dpmcore.security.redirect.strategy.CompositeRedirectStrategy
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

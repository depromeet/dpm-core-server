package com.server.dpmcore.security.redirect.strategy

import com.server.dpmcore.common.constant.Profile
import com.server.dpmcore.security.properties.SecurityProperties
import com.server.dpmcore.security.redirect.model.RedirectContext

class SwaggerRedirectStrategy(
    private val properties: SecurityProperties,
) : RedirectStrategy {
    override fun supports(context: RedirectContext): Boolean =
        (context.profile == Profile.LOCAL) && (context.requestUrl?.startsWith("localhost") == true)

    override fun resolve(context: RedirectContext): String = properties.redirect.swaggerUrl
}

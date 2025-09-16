package com.server.dpmcore.security.redirect.strategy

import com.server.dpmcore.security.properties.SecurityProperties
import com.server.dpmcore.security.redirect.model.RedirectContext
import com.server.dpmcore.security.redirect.validator.RedirectValidator
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class ErrorRedirectStrategy(
    private val properties: SecurityProperties,
    private val validator: RedirectValidator,
) : RedirectStrategy {
    override fun supports(context: RedirectContext): Boolean = context.error != null

    override fun resolve(context: RedirectContext): String {
        val url =
            properties.redirect.restrictedRedirectUrl +
                "?error=true&exception=" +
                URLEncoder.encode(
                    context.error,
                    StandardCharsets.UTF_8,
                )

        return validator.validate(url)
    }
}

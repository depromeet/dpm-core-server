package core.application.security.redirect.strategy

import core.application.security.redirect.model.RedirectContext

interface RedirectStrategy {
    fun supports(context: RedirectContext): Boolean

    fun resolve(context: RedirectContext): String
}

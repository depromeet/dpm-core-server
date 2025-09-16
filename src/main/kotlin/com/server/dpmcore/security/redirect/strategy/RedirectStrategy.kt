package com.server.dpmcore.security.redirect.strategy

import com.server.dpmcore.security.redirect.model.RedirectContext

interface RedirectStrategy {
    fun supports(context: RedirectContext): Boolean

    fun resolve(context: RedirectContext): String
}

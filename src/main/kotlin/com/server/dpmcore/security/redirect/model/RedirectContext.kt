package com.server.dpmcore.security.redirect.model

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.common.constant.Profile
import com.server.dpmcore.security.properties.SecurityProperties

data class RedirectContext(
    val authority: AuthorityType,
    val profile: Profile? = null,
    val requestUrl: String? = null,
    val intent: LoginIntent,
    val originalUrl: String? = null,
    val error: String? = null,
    val target: (SecurityProperties) -> String? = { null },
)

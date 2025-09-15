package com.server.dpmcore.security.redirect

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.common.constant.Profile

data class RedirectRule(
    val authority: AuthorityType,
    val profile: Profile,
    val requestURL: String,
    val target: String,
)

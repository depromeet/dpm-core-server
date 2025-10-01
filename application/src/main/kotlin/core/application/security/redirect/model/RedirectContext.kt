package core.application.security.redirect.model

import core.application.common.constant.Profile
import core.application.security.properties.SecurityProperties
import core.domain.authority.enums.AuthorityType

data class RedirectContext(
    val authority: AuthorityType,
    val profile: Profile? = null,
    val requestUrl: String? = null,
    val intent: LoginIntent,
    val originalUrl: String? = null,
    val error: String? = null,
    val target: (SecurityProperties) -> String? = { null },
)

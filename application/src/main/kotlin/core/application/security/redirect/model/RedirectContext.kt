package core.application.security.redirect.model

import core.application.common.constant.Profile
import core.application.security.properties.SecurityProperties
import core.domain.authorization.vo.RoleType

data class RedirectContext(
    val role: RoleType? = null,
    val profile: Profile? = null,
    val requestUrl: String? = null,
    val intent: LoginIntent,
    val originalUrl: String? = null,
    val error: String? = null,
    val target: (SecurityProperties) -> String? = { null },
)

package com.server.dpmcore.security.redirect.strategy

import com.server.dpmcore.common.constant.Profile
import com.server.dpmcore.security.properties.SecurityProperties
import com.server.dpmcore.security.redirect.model.RedirectContext

/**
 * `LOCAL` 환경에서 Swagger 접속 시 Swagger URL로 리다이렉트하는 전략입니다.
 *
 * application.yml에 설정된 `security.redirect.swagger-url` 값을 사용합니다.
 *
 * @author LeeHanEum
 * @since 2025.09.17
 */
class SwaggerRedirectStrategy(
    private val properties: SecurityProperties,
) : RedirectStrategy {
    override fun supports(context: RedirectContext): Boolean =
        (context.profile == Profile.LOCAL) && (context.requestUrl?.startsWith("localhost") == true)

    override fun resolve(context: RedirectContext): String = properties.redirect.swaggerUrl
}

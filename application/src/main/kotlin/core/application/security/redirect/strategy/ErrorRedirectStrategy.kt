package core.application.security.redirect.strategy

import core.application.security.properties.SecurityProperties
import core.application.security.redirect.model.RedirectContext
import core.application.security.redirect.validator.RedirectValidator
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 리다이렉트 과정에서 오류가 발생했을 때 동작하는 별도의 전략 컴포넌트입니다.
 *
 * 모든 리다이렉트 전략의 최종 fallback 으로 동작하는 컴포넌트입니다.
 *
 * 따라서 CompositeRedirectStrategy 에서 관리되지 않으며 별도의 컴포넌트로 존재합니다.
 *
 * @author LeeHanEum
 * @since 2025.09.17
 */
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

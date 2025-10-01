package core.application.security.redirect.configuration

import core.application.security.properties.SecurityProperties
import core.application.security.redirect.strategy.CompositeRedirectStrategy
import core.application.security.redirect.strategy.ErrorRedirectStrategy
import core.application.security.redirect.strategy.LocalRedirectStrategy
import core.application.security.redirect.strategy.ServerRedirectStrategy
import core.application.security.redirect.strategy.SwaggerRedirectStrategy
import core.application.security.redirect.validator.RedirectValidator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedirectConfig(
    private val properties: SecurityProperties,
    private val validator: RedirectValidator,
    private val errorStrategy: ErrorRedirectStrategy,
) {
    @Bean
    fun redirectStrategy(): CompositeRedirectStrategy =
        CompositeRedirectStrategy(
            listOf(
                ServerRedirectStrategy(properties, validator, errorStrategy),
                LocalRedirectStrategy(properties, validator, errorStrategy),
                SwaggerRedirectStrategy(properties),
                // TODO: 보호된 자원 접근 시 리다이렉트 전략 추가
            ),
        )
}

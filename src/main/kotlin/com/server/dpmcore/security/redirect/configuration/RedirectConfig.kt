package com.server.dpmcore.security.redirect.configuration

import com.server.dpmcore.security.properties.SecurityProperties
import com.server.dpmcore.security.redirect.strategy.CompositeRedirectStrategy
import com.server.dpmcore.security.redirect.strategy.ErrorRedirectStrategy
import com.server.dpmcore.security.redirect.strategy.LocalRedirectStrategy
import com.server.dpmcore.security.redirect.strategy.ServerRedirectStrategy
import com.server.dpmcore.security.redirect.strategy.SwaggerRedirectStrategy
import com.server.dpmcore.security.redirect.validator.RedirectValidator
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

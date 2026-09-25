package core.application.common.configuration

import core.application.security.resolver.CurrentLoginMethodArgumentResolver
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val currentMemberIdArgumentResolver: CurrentMemberIdArgumentResolver,
    private val currentLoginMethodArgumentResolver: CurrentLoginMethodArgumentResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentMemberIdArgumentResolver)
        resolvers.add(currentLoginMethodArgumentResolver)
    }
}

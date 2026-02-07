package core.application.security.configuration

import core.application.security.properties.CorsProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class CorsRegistryConfig(
    private val corsProperties: CorsProperties,
) {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedHeaders = listOf("*")
                allowedMethods = listOf("*")
                allowedOrigins = corsProperties.allowedOrigins
                allowCredentials = true
            }

        val source = UrlBasedCorsConfigurationSource()
        // Apply CORS to API endpoints
        source.registerCorsConfiguration("/api/**", configuration)
        // Apply CORS to OAuth2 login endpoints (critical for cookie-based authentication)
        source.registerCorsConfiguration("/login/**", configuration)
        source.registerCorsConfiguration("/v1/**", configuration)
        source.registerCorsConfiguration("/v2/**", configuration)
        source.registerCorsConfiguration("/oauth2/**", configuration)
        return source
    }
}

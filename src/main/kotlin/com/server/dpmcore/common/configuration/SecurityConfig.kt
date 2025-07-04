package com.server.dpmcore.common.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        disabledConfigurations(httpSecurity)
        configurationSessionManagement(httpSecurity)
        configurationCors(httpSecurity)
        configureAuthorizeHttpRequests(httpSecurity)
        return httpSecurity.build()
    }

    private fun disabledConfigurations(httpSecurity: HttpSecurity) {
        httpSecurity
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
    }

    private fun configurationSessionManagement(httpSecurity: HttpSecurity) {
        httpSecurity
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
    }

    private fun configurationCors(httpSecurity: HttpSecurity) {
        httpSecurity
            .cors { it.configurationSource(corsConfigurationSource()) }
    }

    private fun corsConfigurationSource(): CorsConfigurationSource {
        return CorsConfigurationSource { _ ->
            CorsConfiguration().apply {
                allowedHeaders = listOf("*")
                allowedMethods = listOf("*")
                allowedOriginPatterns = listOf(
                    "https://localhost:3000",
                )
                allowCredentials = true
            }
        }
    }

    private fun configureAuthorizeHttpRequests(httpSecurity: HttpSecurity) {
        httpSecurity
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(*SWAGGER_PATTERNS).permitAll()
                    .requestMatchers(*STATIC_RESOURCES_PATTERNS).permitAll()
                    .requestMatchers(*PERMIT_ALL_PATTERNS).permitAll()
                    .anyRequest().authenticated()
            }
    }

    companion object {
        private val SWAGGER_PATTERNS = arrayOf(
            "/swagger-ui/**", "/actuator/**", "/v3/api-docs/**"
        )
        private val STATIC_RESOURCES_PATTERNS = arrayOf(
            "/images/**", "/css/**", "/js/**", "/static/**"
        )
        private val PERMIT_ALL_PATTERNS = arrayOf(
            "/v1/**", "/login", "/error"
        )
    }
}

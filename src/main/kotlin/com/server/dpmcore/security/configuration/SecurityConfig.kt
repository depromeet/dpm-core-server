package com.server.dpmcore.security.configuration

import com.server.dpmcore.security.oauth.token.JwtAuthenticationFilter
import com.server.dpmcore.security.properties.SecurityProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val securityProperties: SecurityProperties,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authorizationRequestRepository: AuthorizationRequestRepository<OAuth2AuthorizationRequest>,
    private val defaultOAuth2UserService: DefaultOAuth2UserService,
    private val authenticationSuccessHandler: AuthenticationSuccessHandler,
    private val authenticationFailureHandler: AuthenticationFailureHandler,
    private val logoutSuccessHandler: LogoutSuccessHandler,
) {
    @Bean
    fun filterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        disabledConfigurations(httpSecurity)
        configurationSessionManagement(httpSecurity)
        configurationCors(httpSecurity)
        configureAuthorizeHttpRequests(httpSecurity)
        configurationOAuth2Login(httpSecurity)
        configurationLogout(httpSecurity)
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

    private fun corsConfigurationSource(): CorsConfigurationSource =
        CorsConfigurationSource { _ ->
            CorsConfiguration().apply {
                allowedHeaders = listOf("*")
                allowedMethods = listOf("*")
                allowedOriginPatterns =
                    listOf(
                        "https://localhost:3000",
                        "https://local.depromeet-core.shop",
                        "https://client.depromeet-core.shop",
                        "https://admin.depromeet-core.shop",
                        "https://api.depromeet-core.shop",
                        "https://core.depromeet.com",
                        "https://admin.depromeet.com",
                        "https://api.depromeet.com",
                    )
                allowCredentials = true
            }
        }

    private fun configureAuthorizeHttpRequests(httpSecurity: HttpSecurity) {
        httpSecurity
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*SWAGGER_PATTERNS)
                    .permitAll()
                    .requestMatchers(*STATIC_RESOURCES_PATTERNS)
                    .permitAll()
                    .requestMatchers(*PERMIT_ALL_PATTERNS)
                    .permitAll()
                    .requestMatchers("/logout", "/v1/members/withdraw")
                    .hasAnyRole("GUEST", "DEEPER", "ORGANIZER")
                    .anyRequest()
                    .authenticated()
            }
    }

    private fun configurationOAuth2Login(httpSecurity: HttpSecurity) {
        httpSecurity
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .oauth2Login { oauth2 ->
                oauth2
                    .authorizationEndpoint {
                        it.authorizationRequestRepository(authorizationRequestRepository)
                    }.userInfoEndpoint {
                        it.userService(defaultOAuth2UserService)
                    }.successHandler(authenticationSuccessHandler)
                    .failureHandler(authenticationFailureHandler)
            }
    }

    private fun configurationLogout(httpSecurity: HttpSecurity) {
        httpSecurity.logout { logout ->
            logout.logoutUrl(securityProperties.logoutUrl)
            logout.logoutSuccessHandler(logoutSuccessHandler)
        }
    }

    companion object {
        private val SWAGGER_PATTERNS =
            arrayOf(
                "/swagger-ui/**",
                "/actuator/**",
                "/v3/api-docs/**",
            )
        private val STATIC_RESOURCES_PATTERNS =
            arrayOf(
                "/images/**",
                "/css/**",
                "/js/**",
                "/static/**",
            )
        private val PERMIT_ALL_PATTERNS =
            arrayOf(
                "/v1/reissue",
                "/login/kakao",
                "/error",
            )
    }
}

package core.application.security.configuration

import core.application.security.handler.CustomAuthenticationEntryPoint
import core.application.security.oauth.client.CustomOAuth2AccessTokenResponseClient
import core.application.security.oauth.token.JwtAuthenticationFilter
import core.application.security.properties.SecurityProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler

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
    private val customOAuth2AccessTokenResponseClient: CustomOAuth2AccessTokenResponseClient,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
) {
    @Bean
    fun filterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        disabledConfigurations(httpSecurity)
        configurationSessionManagement(httpSecurity)
        configurationCors(httpSecurity)
        configureExceptionHandling(httpSecurity)
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
            .cors { }
    }

    private fun configureExceptionHandling(httpSecurity: HttpSecurity) {
        httpSecurity.exceptionHandling { handling ->
            handling.authenticationEntryPoint(customAuthenticationEntryPoint)
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
                    .requestMatchers("/logout")
                    .permitAll()
                    .requestMatchers("/v1/members/withdraw")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
    }

    private fun configurationOAuth2Login(httpSecurity: HttpSecurity) {
        httpSecurity
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .oauth2Login { oauth2 ->
                oauth2
                    .tokenEndpoint {
                        it.accessTokenResponseClient(customOAuth2AccessTokenResponseClient)
                    }.authorizationEndpoint {
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

    @Bean
    fun passwordEncoder(): PasswordEncoder = org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()


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
                // API endpoints (under /v1/)
                "/v1/reissue",
                "/v1/**",
                "/v2/**",
                // OAuth2 endpoints
                "/oauth2/**",
                "oauth2/**",
                "/login/oauth2/**",
                // Login entry points (specific paths before wildcards)
                "/login/kakao",
                "/login/apple",
                "/login/auth/apple",
                "/login/email",
                // General login paths (must come after specific paths)
                "/login/**",
                "/login",
                "/error",
            )
    }
}

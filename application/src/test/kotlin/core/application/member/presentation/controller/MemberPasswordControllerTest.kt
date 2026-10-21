package core.application.member.presentation.controller

import core.application.common.exception.GlobalExceptionHandler
import core.application.member.application.service.MemberApprovalService
import core.application.member.application.service.MemberCommandService
import core.application.member.application.service.MemberNameHashTypeValidator
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.auth.AppleAuthService
import core.application.member.application.service.auth.EmailPasswordAuthService
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import core.domain.member.vo.MemberId
import jakarta.servlet.Filter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringJUnitConfig(MemberPasswordControllerTest.Config::class)
@WebAppConfiguration
class MemberPasswordControllerTest {
    @Autowired lateinit var context: WebApplicationContext

    @Autowired lateinit var passwords: EmailPasswordAuthService

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc =
            MockMvcBuilders.webAppContextSetup(context)
                .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(context.getBean("springSecurityFilterChain", Filter::class.java))
                .build()
        clearInvocations(passwords)
    }

    @Test
    fun `익명은 비밀번호를 변경할 수 없다`() {
        mvc.perform(patch(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized)
        verifyNoInteractions(passwords)
    }

    @Test
    fun `요청에 다른 회원을 지정해도 인증된 본인의 비밀번호만 변경한다`() {
        mvc.perform(authenticated(patch(PATH).param("memberId", "8").contentType(MediaType.APPLICATION_JSON).content(BODY)))
            .andExpect(status().isOk)
        verify(passwords).setPassword(MemberId(7), "old-password", "new-password")
    }

    private fun authenticated(request: MockHttpServletRequestBuilder) =
        request.requestAttr(
            RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
            SecurityContextImpl(UsernamePasswordAuthenticationToken("7", null, emptyList())),
        )

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import(MemberController::class, GlobalExceptionHandler::class)
    class Config : WebMvcConfigurer {
        @Bean fun queries(): MemberQueryService = mock(MemberQueryService::class.java)

        @Bean fun commands(): MemberCommandService = mock(MemberCommandService::class.java)

        @Bean fun approvals(): MemberApprovalService = mock(MemberApprovalService::class.java)

        @Bean fun names(): MemberNameHashTypeValidator = mock(MemberNameHashTypeValidator::class.java)

        @Bean fun apple(): AppleAuthService = mock(AppleAuthService::class.java)

        @Bean fun passwords(): EmailPasswordAuthService = mock(EmailPasswordAuthService::class.java)

        @Bean fun injector(): JwtTokenInjector = mock(JwtTokenInjector::class.java)

        @Bean fun devices(): DeviceIdResolver = mock(DeviceIdResolver::class.java)

        override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
            resolvers.add(CurrentMemberIdArgumentResolver())
        }

        @Bean
        fun security(http: HttpSecurity): SecurityFilterChain =
            http.csrf { it.disable() }
                .securityContext { it.securityContextRepository(RequestAttributeSecurityContextRepository()) }
                .authorizeHttpRequests { it.anyRequest().permitAll() }.build()
    }

    private companion object {
        const val PATH = "/v1/members/password"
        const val BODY = """{"oldPassword":"old-password","newPassword":"new-password","confirmPassword":"new-password"}"""
    }
}

package core.application.member.presentation.controller

import core.application.common.exception.GlobalExceptionHandler
import core.application.member.application.service.KakaoNativeAccountLinkService
import core.application.member.application.service.MemberAccountLinkRequestService
import core.application.member.presentation.response.MemberAccountResponse
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.resolver.CurrentLoginIdentityArgumentResolver
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import core.domain.member.enums.LoginMethod
import core.domain.member.vo.LoginIdentity
import jakarta.servlet.Filter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringJUnitConfig(KakaoNativeAccountLinkControllerTest.Config::class)
@WebAppConfiguration
class KakaoNativeAccountLinkControllerTest {
    @Autowired lateinit var context: WebApplicationContext

    @Autowired lateinit var requests: MemberAccountLinkRequestService

    @Autowired lateinit var links: KakaoNativeAccountLinkService

    @Autowired lateinit var tokens: JwtTokenProvider
    private lateinit var mvc: MockMvc
    private val identity = LoginIdentity(LoginMethod.APPLE, 11)

    @BeforeEach
    fun setup() {
        mvc =
            MockMvcBuilders.webAppContextSetup(context)
                .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(context.getBean("springSecurityFilterChain", Filter::class.java)).build()
        clearInvocations(requests, links)
        `when`(tokens.getLoginIdentity("service-token")).thenReturn(identity)
        `when`(requests.startNativeKakao(7, identity)).thenReturn(STATE)
        `when`(links.complete(7, identity, STATE, "provider-token")).thenReturn(MemberAccountResponse(emptyList()))
    }

    @Test
    fun `익명은 시작과 완료를 호출할 수 없다`() {
        mvc.perform(post("$PATH/authorization")).andExpect(status().isUnauthorized)
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized)
        verifyNoInteractions(requests, links)
    }

    @Test
    fun `일반 인증 회원이 본인 식별자로 연결을 시작하고 완료한다`() {
        mvc.perform(authenticated(post("$PATH/authorization"))).andExpect(status().isOk).andExpect(jsonPath("$.data.state").value(STATE))
        mvc.perform(authenticated(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))).andExpect(status().isOk)
        verify(requests).startNativeKakao(7, identity)
        verify(links).complete(7, identity, STATE, "provider-token")
    }

    @Test
    fun `state와 카카오 토큰 누락 및 잘못된 길이는 거절한다`() {
        listOf("{}", """{"state":"$STATE"}""", """{"state":"short","accessToken":"provider-token"}""", """{"state":"$STATE","accessToken":" "}""").forEach { body ->
            mvc.perform(authenticated(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))).andExpect(status().isBadRequest)
        }
        verifyNoInteractions(links)
    }

    private fun authenticated(request: MockHttpServletRequestBuilder) =
        request.requestAttr(
            RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
            SecurityContextImpl(UsernamePasswordAuthenticationToken("7", "service-token", emptyList())),
        )

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import(KakaoNativeAccountLinkController::class, GlobalExceptionHandler::class)
    class Config : WebMvcConfigurer {
        @Bean fun requests(): MemberAccountLinkRequestService = mock(MemberAccountLinkRequestService::class.java)

        @Bean fun links(): KakaoNativeAccountLinkService = mock(KakaoNativeAccountLinkService::class.java)

        @Bean fun tokens(): JwtTokenProvider = mock(JwtTokenProvider::class.java)

        override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
            resolvers.add(CurrentMemberIdArgumentResolver())
            resolvers.add(CurrentLoginIdentityArgumentResolver(tokens()))
        }

        @Bean
        fun security(http: HttpSecurity): SecurityFilterChain =
            http.csrf { it.disable() }
                .securityContext { it.securityContextRepository(RequestAttributeSecurityContextRepository()) }
                .authorizeHttpRequests { it.anyRequest().permitAll() }.build()
    }

    private companion object {
        const val PATH = "/v3/members/me/accounts/KAKAO/native"
        const val STATE = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG"
        const val BODY = """{"state":"$STATE","accessToken":"provider-token"}"""
    }
}

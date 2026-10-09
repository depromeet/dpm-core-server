package core.application.member.presentation.controller

import core.application.common.exception.GlobalExceptionHandler
import core.application.member.application.service.MemberProfileService
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberProfile
import core.domain.member.port.outbound.MemberProfilePersistencePort
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Instant

@SpringJUnitConfig(MemberProfileControllerTest.Config::class)
@WebAppConfiguration
class MemberProfileControllerTest {
    @Autowired lateinit var context: WebApplicationContext

    @Autowired lateinit var profiles: MemberProfilePersistencePort

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc =
            MockMvcBuilders.webAppContextSetup(context)
                .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(context.getBean("springSecurityFilterChain", Filter::class.java))
                .build()
        clearInvocations(profiles)
        `when`(profiles.findProfile(7)).thenReturn(profile())
        `when`(profiles.lockProfile(7)).thenReturn(profile())
        `when`(profiles.completeProfile(7, "홍길동", MemberPart.WEB)).thenReturn(true)
    }

    @Test
    fun `익명은 조회와 입력을 사용할 수 없다`() {
        mvc.perform(get(PATH)).andExpect(status().isForbidden)
        mvc.perform(patch(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden)
        verifyNoInteractions(profiles)
    }

    @Test
    fun `별도 권한 없는 인증 회원은 본인 상태를 조회하고 두 필드를 입력한다`() {
        mvc.perform(authenticated(get(PATH))).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.profileCompletionRequired").value(true))
        mvc.perform(request(BODY)).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("홍길동"))
            .andExpect(jsonPath("$.data.part").value("WEB"))
            .andExpect(jsonPath("$.data.profileCompletionRequired").value(false))
        verify(profiles).completeProfile(7, "홍길동", MemberPart.WEB)
    }

    @Test
    fun `이름 파트 누락과 미배정 잘못된 이름 다른 회원 지정은 거절한다`() {
        listOf(
            "{}",
            """{"name":"홍길동"}""",
            """{"part":"WEB"}""",
            """{"name":null,"part":"WEB"}""",
            """{"name":"Hong","part":"WEB"}""",
            """{"name":"홍길동","part":"UNASSIGNED"}""",
            """{"name":"홍길동","part":"WEB","memberId":8}""",
        ).forEach { body ->
            mvc.perform(request(body)).andExpect(status().isBadRequest)
        }
        verifyNoInteractions(profiles)
    }

    @Test
    fun `완료 후 다른 값은 공통 응답 형식의 409다`() {
        `when`(profiles.lockProfile(7)).thenReturn(profile().copy(completedAt = Instant.now()))
        mvc.perform(request("""{"name":"김길동","part":"WEB"}"""))
            .andExpect(status().isConflict).andExpect(jsonPath("$.code").value("MEMBER-409-41"))
    }

    private fun profile() = MemberProfile(7, "홍길동", MemberPart.WEB, MemberStatus.PENDING, null, null)

    private fun request(body: String) = authenticated(patch(PATH).contentType(MediaType.APPLICATION_JSON).content(body))

    private fun authenticated(request: MockHttpServletRequestBuilder) =
        request.requestAttr(
            RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
            SecurityContextImpl(UsernamePasswordAuthenticationToken("7", null, emptyList())),
        )

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import(MemberProfileController::class, MemberProfileService::class, GlobalExceptionHandler::class)
    class Config : WebMvcConfigurer {
        @Bean fun profiles(): MemberProfilePersistencePort = mock(MemberProfilePersistencePort::class.java)

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
        const val PATH = "/v3/members/me/profile"
        const val BODY = """{"name":"홍길동","part":"WEB"}"""
    }
}

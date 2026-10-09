package core.application.member.presentation.controller

import core.application.common.exception.GlobalExceptionHandler
import core.application.member.application.service.MemberActivationInitializer
import core.application.member.application.service.MemberApprovalService
import core.application.member.application.service.MemberCommandService
import core.application.member.application.service.MemberNameHashTypeValidator
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.auth.AppleAuthService
import core.application.member.application.service.auth.EmailPasswordAuthService
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberCohortPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.port.outbound.MemberTeamPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
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
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringJUnitConfig(MemberApprovalControllerTest.Config::class)
@WebAppConfiguration
class MemberApprovalControllerTest {
    @Autowired lateinit var context: WebApplicationContext

    @Autowired lateinit var members: MemberPersistencePort

    @Autowired lateinit var initializer: MemberActivationInitializer

    @Autowired lateinit var cohorts: CohortQueryUseCase

    @Autowired lateinit var roleQueries: RoleQueryUseCase

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc =
            MockMvcBuilders.webAppContextSetup(context)
                .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(context.getBean("springSecurityFilterChain", Filter::class.java))
                .build()
        clearInvocations(members, initializer, cohorts, roleQueries)
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(19))
        `when`(roleQueries.findIdByName("DEEPER")).thenReturn(1)
        `when`(members.lockApprovalTargets(listOf(1L))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet())))
    }

    @Test
    fun `익명과 조회 수정 권한만 있는 사용자는 승인하지 못한다`() {
        mvc.perform(patch(PATH).contentType(MediaType.APPLICATION_JSON).content("""{"members":[1]}"""))
            .andExpect(status().isForbidden)
        listOf("read:member", "update:member", "update:authorization").forEach {
            mvc.perform(request("""{"members":[1]}""", it)).andExpect(status().isForbidden)
        }
        verifyNoInteractions(members, initializer, cohorts, roleQueries)
    }

    @Test
    fun `승인 권한이 있는 요청은 기존 응답 형식으로 반환한다`() {
        mvc.perform(request("""{"members":[1]}"""))
            .andExpect(status().isOk).andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
        verify(members).lockApprovalTargets(listOf(1))
    }

    @Test
    fun `엄격하지 않은 식별자와 알 수 없는 필드는 400으로 거절한다`() {
        listOf("1.9", "1.0", "9223372036854775808", "\"1\"", "true", "{}", "[]").forEach { value ->
            mvc.perform(request("""{"members":[$value]}"""))
                .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("GLOBAL-400-01"))
        }
        mvc.perform(request("""{"members":[1],"part":"SERVER"}"""))
            .andExpect(status().isBadRequest)
        verifyNoInteractions(members, initializer, cohorts, roleQueries)
    }

    @Test
    fun `빈 목록과 잘못된 ID의 입력 오류를 반환한다`() {
        mvc.perform(request("""{"members":[]}"""))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("GLOBAL-400-01"))
        mvc.perform(request("""{"members":[0]}"""))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("MEMBER-400-09"))
        verifyNoInteractions(members, initializer, cohorts, roleQueries)
    }

    @Test
    fun `승인 불가 대상은 도메인 오류를 반환한다`() {
        `when`(members.lockApprovalTargets(listOf(1L))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.PENDING, setOf(18))))
        mvc.perform(request("""{"members":[1]}"""))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("MEMBER-400-10"))
        verifyNoInteractions(initializer, roleQueries)
    }

    @Test
    fun `기존 승인 경로도 승인 권한을 유지하고 중복 ID를 한 번만 처리한다`() {
        mvc.perform(patch("/v1/members/whitelist").contentType(MediaType.APPLICATION_JSON).content("""{"members":[1]}"""))
            .andExpect(status().isForbidden)
        mvc.perform(
            patch("/v1/members/whitelist").contentType(MediaType.APPLICATION_JSON).content("""{"members":[1,1]}""").requestAttr(
                RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
                SecurityContextImpl(UsernamePasswordAuthenticationToken("operator", null, listOf(SimpleGrantedAuthority("create:member")))),
            ),
        ).andExpect(status().isOk).andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
        verify(members).lockApprovalTargets(listOf(1))
    }

    private fun request(
        body: String,
        authority: String = "create:member",
    ): MockHttpServletRequestBuilder =
        patch(PATH).contentType(MediaType.APPLICATION_JSON).content(body).requestAttr(
            RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
            SecurityContextImpl(UsernamePasswordAuthenticationToken("operator", null, listOf(SimpleGrantedAuthority(authority)))),
        )

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import(MemberApprovalController::class, MemberController::class, MemberApprovalService::class, GlobalExceptionHandler::class)
    class Config {
        @Bean fun memberQueries(): MemberQueryService = mock(MemberQueryService::class.java)

        @Bean fun memberCommands(): MemberCommandService = mock(MemberCommandService::class.java)

        @Bean fun nameValidator(): MemberNameHashTypeValidator = mock(MemberNameHashTypeValidator::class.java)

        @Bean fun appleAuth(): AppleAuthService = mock(AppleAuthService::class.java)

        @Bean fun emailAuth(): EmailPasswordAuthService = mock(EmailPasswordAuthService::class.java)

        @Bean fun tokenInjector(): JwtTokenInjector = mock(JwtTokenInjector::class.java)

        @Bean fun deviceResolver(): DeviceIdResolver = mock(DeviceIdResolver::class.java)

        @Bean fun members(): MemberPersistencePort = mock(MemberPersistencePort::class.java)

        @Bean fun memberCohorts(): MemberCohortPersistencePort = mock(MemberCohortPersistencePort::class.java)

        @Bean fun teams(): MemberTeamPersistencePort = mock(MemberTeamPersistencePort::class.java)

        @Bean fun roles(): MemberRolePersistencePort = mock(MemberRolePersistencePort::class.java)

        @Bean fun cohorts(): CohortQueryUseCase = mock(CohortQueryUseCase::class.java)

        @Bean fun roleQueries(): RoleQueryUseCase = mock(RoleQueryUseCase::class.java)

        @Bean fun initializer(): MemberActivationInitializer = mock(MemberActivationInitializer::class.java)

        @Bean
        fun security(http: HttpSecurity): SecurityFilterChain =
            http.csrf { it.disable() }
                .securityContext { it.securityContextRepository(RequestAttributeSecurityContextRepository()) }
                .authorizeHttpRequests { it.anyRequest().permitAll() }
                .build()
    }

    private companion object {
        const val PATH = "/v3/members/whitelist"
    }
}

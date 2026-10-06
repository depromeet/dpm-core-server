package core.application.member.presentation.controller

import core.application.common.exception.GlobalExceptionHandler
import core.application.member.application.service.MemberManagementCommandService
import core.application.member.application.service.MemberManagementQueryService
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.port.outbound.MemberTeamPersistencePort
import jakarta.servlet.Filter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
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

@SpringJUnitConfig(MemberManagementUpdateControllerTest.Config::class)
@WebAppConfiguration
class MemberManagementUpdateControllerTest {
    @Autowired lateinit var context: WebApplicationContext

    @Autowired lateinit var members: MemberPersistencePort

    @Autowired lateinit var teams: MemberTeamPersistencePort

    @Autowired lateinit var roles: MemberRolePersistencePort

    @Autowired lateinit var cohorts: CohortQueryUseCase

    @Autowired lateinit var roleQueries: RoleQueryUseCase

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc =
            MockMvcBuilders.webAppContextSetup(context)
                .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(context.getBean("springSecurityFilterChain", Filter::class.java))
                .build()
        clearInvocations(members, teams, roles, cohorts, roleQueries)
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(19))
        `when`(members.lockApprovedManagementMemberIds(listOf(1L), 19)).thenReturn(listOf(1L))
        `when`(members.lockApprovedManagementMemberIds(listOf(1L, 2L), 19)).thenReturn(listOf(1L, 2L))
        `when`(roleQueries.findIdByName("CORE")).thenReturn(3)
    }

    @Test
    fun `익명과 조회 권한만 있는 사용자의 단건 일괄 수정을 거절한다`() {
        mvc.perform(patch("$PATH/1").contentType(MediaType.APPLICATION_JSON).content("""{"part":"SERVER"}"""))
            .andExpect(status().isForbidden)
        mvc.perform(request("1", """{"part":"SERVER"}""", "read:member"))
            .andExpect(status().isForbidden)
        mvc.perform(request("bulk", """{"memberIds":[1],"changes":{"part":"SERVER"}}""", "read:member"))
            .andExpect(status().isForbidden)
        verifyNoInteractions(members, teams, roles, cohorts, roleQueries)
    }

    @Test
    fun `타입 포함 요청은 추가 권한 없으면 다른 필드도 수정하지 않는다`() {
        mvc.perform(request("1", """{"part":"SERVER","memberType":"CORE"}"""))
            .andExpect(status().isForbidden)
        mvc.perform(request("bulk", """{"memberIds":[1],"changes":{"memberType":"UNASSIGNED"}}"""))
            .andExpect(status().isForbidden)
        mvc.perform(request("1", """{"memberType":"CORE"}""", "update:authorization"))
            .andExpect(status().isForbidden)
        verifyNoInteractions(members, teams, roles, cohorts, roleQueries)
    }

    @Test
    fun `두 권한이 있으면 타입을 수정하고 기존 성공 응답 형식을 반환한다`() {
        mvc.perform(request("1", """{"memberType":"CORE"}""", "update:member", "update:authorization"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
        verify(roles).replaceCurrentCohortRoles(listOf(1L), 19, 3)
    }

    @Test
    fun `단건의 여러 컬럼과 bulk 경로의 한 컬럼을 각각 바인딩한다`() {
        mvc.perform(request("1", """{"part":"UNASSIGNED","status":"INACTIVE"}"""))
            .andExpect(status().isOk)
        verify(members).updateManagementFields(listOf(1L), true, null, MemberStatus.INACTIVE, emptySet())
        mvc.perform(request("bulk", """{"memberIds":[2,1],"changes":{"part":"SERVER"}}"""))
            .andExpect(status().isOk)
        verify(members).updateManagementFields(listOf(1L, 2L), true, MemberPart.SERVER, null, emptySet())
    }

    @Test
    fun `여러 컬럼 일괄 수정과 빈 요청 잘못된 ID를 일관된 400으로 반환한다`() {
        listOf(
            """{"memberIds":[1],"changes":{}}""",
            """{"memberIds":[1],"changes":{"part":"SERVER","status":"INACTIVE"}}""",
            """{"memberIds":[],"changes":{"part":"SERVER"}}""",
            """{"memberIds":[1,1],"changes":{"part":"SERVER"}}""",
            """{"memberIds":[null],"changes":{"part":"SERVER"}}""",
            """{"memberIds":[0],"changes":{"part":"SERVER"}}""",
            """{"memberIds":[1]}""",
        ).forEach { body -> mvc.perform(request("bulk", body)).andExpect(status().isBadRequest) }
        mvc.perform(request("0", """{"part":"SERVER"}""")).andExpect(status().isBadRequest)
        mvc.perform(request("1", "{}")).andExpect(status().isBadRequest)
        verifyNoInteractions(members, teams, roles, cohorts, roleQueries)
    }

    @Test
    fun `허용하지 않은 닉네임 이메일 컬럼과 잘못된 필드 값을 거절한다`() {
        listOf(
            """{"part":"SERVER","name":"변경"}""",
            """{"email":"test@example.com"}""",
            """{"status":"AT_RISK"}""",
            """{"status":"PENDING"}""",
            """{"memberType":"MASTER"}""",
            """{"part":"UNKNOWN"}""",
            """{"teamId":-1}""",
        ).forEach { body ->
            mvc.perform(request("1", body, "update:member", "update:authorization"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("GLOBAL-400-01"))
        }
        mvc.perform(request("bulk", """{"memberIds":[1],"changes":{"part":"SERVER","nickname":"변경"}}"""))
            .andExpect(status().isBadRequest)
        verifyNoInteractions(members, teams, roles, cohorts, roleQueries)
    }

    @Test
    fun `현재 기수 승인 대상이 아니면 변경 호출을 막고 도메인 오류를 반환한다`() {
        `when`(members.lockApprovedManagementMemberIds(listOf(1L), 19)).thenReturn(emptyList())
        mvc.perform(request("1", """{"part":"SERVER"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MEMBER-400-07"))
        verify(members).lockApprovedManagementMemberIds(listOf(1L), 19)
        verifyNoMoreInteractions(members)
        verifyNoInteractions(teams, roles, roleQueries)
    }

    private fun request(
        path: String,
        body: String,
        vararg authorities: String = arrayOf("update:member"),
    ): MockHttpServletRequestBuilder =
        patch("$PATH/$path").contentType(MediaType.APPLICATION_JSON).content(body).requestAttr(
            RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
            SecurityContextImpl(UsernamePasswordAuthenticationToken("operator", null, authorities.map(::SimpleGrantedAuthority))),
        )

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import(MemberManagementController::class, MemberManagementCommandService::class, GlobalExceptionHandler::class)
    class Config {
        @Bean fun queryService(): MemberManagementQueryService = mock(MemberManagementQueryService::class.java)

        @Bean fun members(): MemberPersistencePort = mock(MemberPersistencePort::class.java)

        @Bean fun teams(): MemberTeamPersistencePort = mock(MemberTeamPersistencePort::class.java)

        @Bean fun roles(): MemberRolePersistencePort = mock(MemberRolePersistencePort::class.java)

        @Bean fun cohorts(): CohortQueryUseCase = mock(CohortQueryUseCase::class.java)

        @Bean fun cohortPort(): CohortPersistencePort = mock(CohortPersistencePort::class.java)

        @Bean fun roleQueries(): RoleQueryUseCase = mock(RoleQueryUseCase::class.java)

        @Bean
        fun security(http: HttpSecurity): SecurityFilterChain =
            http.csrf { it.disable() }
                .securityContext { it.securityContextRepository(RequestAttributeSecurityContextRepository()) }
                .authorizeHttpRequests { it.anyRequest().permitAll() }
                .build()
    }

    private companion object {
        const val PATH = "/v3/members"
    }
}

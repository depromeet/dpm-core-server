package core.application.member.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import core.application.cohort.application.service.CohortCommandService
import core.application.cohort.application.service.CohortQueryService
import core.application.cohort.presentation.controller.CohortController
import core.application.common.exception.GlobalExceptionHandler
import core.application.member.application.service.MemberManagementCommandService
import core.application.member.application.service.MemberManagementQueryService
import core.application.member.presentation.request.MemberManagementRequest
import core.application.member.presentation.request.MemberManagementRequest.ActivityStatus
import core.application.member.presentation.response.MemberManagementResponse
import core.application.member.presentation.response.MemberOverviewResponse
import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.cohort.port.outbound.query.CohortTeamQueryModel
import core.domain.member.enums.MemberStatus
import io.swagger.v3.core.converter.ModelConverters
import jakarta.servlet.Filter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc

/** 실제 MVC 요청과 메서드 보안 프록시를 검증한다. JWT 검증 대신 요청별 인증 객체를 공급한다. */
@SpringJUnitConfig(MemberManagementControllerTest.Config::class)
@WebAppConfiguration
class MemberManagementControllerTest {
    @Autowired lateinit var context: WebApplicationContext

    @Autowired lateinit var service: MemberManagementQueryService

    @Autowired lateinit var cohorts: CohortQueryService

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc =
            MockMvcBuilders.webAppContextSetup(context)
                .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(context.getBean("springSecurityFilterChain", Filter::class.java))
                .build()
        clearInvocations(service, cohorts)
    }

    @Test
    fun `익명 및 조회 권한 없는 요청을 거절하고 서비스 호출을 막는다`() {
        mvc.perform(get(PATH)).andExpect(status().isForbidden)
        mvc.perform(authenticated("read:attendance")).andExpect(status().isForbidden)
        verifyNoInteractions(service)
    }

    @Test
    fun `조회 권한이 있으면 닉네임 표시 이메일과 중복 여부를 기존 응답 형식으로 반환한다`() {
        val member = MemberManagementResponse.MemberSummary(1, 19, "휘", "kakao@example.com", "SERVER", "DEEPER", 1, MemberStatus.ACTIVE, false, null, true, null)
        val missingEmail = member.copy(memberId = 2, name = "별명", email = null, duplicateSuspected = false)
        val response = MemberManagementResponse(19, MemberManagementResponse.Summary(2, 2, 0, 0, 0, 0, 0), 2, 1, 20, null, listOf(member, missingEmail))
        `when`(service.getOverview(MemberManagementRequest())).thenReturn(response)
        val result =
            mvc.perform(authenticated("read:member"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.members[0].name").value("휘"))
                .andExpect(jsonPath("$.data.members[0].email").value("kakao@example.com"))
                .andExpect(jsonPath("$.data.members[0].duplicateSuspected").value(true))
                .andExpect(jsonPath("$.data.members[1].duplicateSuspected").value(false))
                .andReturn()
        assertThat(result.response.contentAsString).contains("\"email\":null")
        assertThat(result.response.contentAsString).doesNotContain("\"signupEmail\":")
    }

    @Test
    fun `v1과 v3의 회원 항목은 Swagger에서 서로 다른 모델로 노출된다`() {
        val legacy = ModelConverters.getInstance().readAll(MemberOverviewResponse::class.java)
        val current = ModelConverters.getInstance().readAll(MemberManagementResponse::class.java)
        assertThat(legacy).containsKey("MemberSummary")
        assertThat(current).containsKeys("MemberManagementSummary", "MemberManagementTotals")
        assertThat(current).doesNotContainKey("MemberSummary")
        val properties = current["MemberManagementSummary"]!!.properties
        assertThat(properties).containsKeys("name", "email", "duplicateSuspected").doesNotContainKey("signupEmail")
        assertThat(properties["name"]!!.description).isEqualTo("닉네임")
        assertThat(properties["email"]!!.nullable).isTrue()
        assertThat(properties["duplicateSuspected"]!!.nullable == true).isFalse()
        assertThat(legacy["MemberSummary"]!!.properties).doesNotContainKeys("email", "signupEmail", "duplicateSuspected")
        val requestProperties = ModelConverters.getInstance().readAll(MemberManagementRequest::class.java)["MemberManagementRequest"]!!.properties
        assertThat(requestProperties).containsKeys("parts", "teamNumbers", "activityStatuses")
            .doesNotContainKeys("part", "teamNumber", "status", "filterValuesValid", "isFilterValuesValid")
        assertThat(ObjectMapper().writeValueAsString(MemberManagementRequest())).doesNotContain("FilterValuesValid", "filterValuesValid")
    }

    @Test
    fun `수료 필터의 후행 빈 값은 Spring 바인딩에서 null 원소가 된다`() {
        mvc.perform(authenticated("read:member").param("graduationStatuses", "NORMAL,"))
            .andExpect(status().isOk)
        val request = mockingDetails(service).invocations.single().arguments.single() as MemberManagementRequest
        assertThat(request.graduationStatuses).containsExactly(AttendanceGraduationStatus.NORMAL, null)
    }

    @Test
    fun `공통 파트와 팀 선택지를 목록의 파트와 팀 번호 필터로 전달한다`() {
        `when`(cohorts.getActiveCohortTeams()).thenReturn(listOf(CohortTeamQueryModel(id = 42, number = 3)))
        val authorities = arrayOf("read:member", "update:attendance")
        val mapper = ObjectMapper()
        val parts =
            mvc.perform(authenticatedRequest("/v3/members/parts", *authorities))
                .andExpect(status().isOk)
                .andReturn().response.contentAsString
        val teams =
            mvc.perform(authenticatedRequest("/v3/cohorts/current/teams", *authorities))
                .andExpect(status().isOk)
                .andReturn().response.contentAsString
        val unassigned = mapper.readTree(parts).path("data").path("parts").single { it.asText() == "UNASSIGNED" }.asText()
        val team = mapper.readTree(teams).path("data").path("teams").single()
        assertThat(team.path("id").asLong()).isEqualTo(42)

        // 공통 응답의 팀 ID가 아니라 number를 목록 필터에 사용한다. 미배정 팀은 0이다.
        listOf(team.path("number").asInt(), 0).forEach { number ->
            mvc.perform(authenticatedRequest(PATH, *authorities).param("parts", unassigned).param("teamNumbers", number.toString()))
                .andExpect(status().isOk)
        }
        val requests = mockingDetails(service).invocations.map { it.arguments.single() as MemberManagementRequest }
        assertThat(requests.map { it.parts }).containsOnly(listOf("UNASSIGNED"))
        assertThat(requests.map { it.teamNumbers }).containsExactly(listOf(3), listOf(0))
    }

    @Test
    fun `여러 필터 값을 반복 파라미터와 쉼표로 전달한다`() {
        mvc.perform(
            authenticated("read:member")
                .param("parts", "WEB", "DESIGN")
                .param("teamNumbers", "0,3")
                .param("activityStatuses", "NORMAL", "INACTIVE"),
        ).andExpect(status().isOk)
        val request = mockingDetails(service).invocations.single().arguments.single() as MemberManagementRequest
        assertThat(request.parts).containsExactly("WEB", "DESIGN")
        assertThat(request.teamNumbers).containsExactly(0, 3)
        assertThat(request.activityStatuses).containsExactly(ActivityStatus.NORMAL, ActivityStatus.INACTIVE)
    }

    @Test
    fun `빈 필터와 최대 페이지 값도 정상적으로 바인딩한다`() {
        mvc.perform(
            authenticated("read:member")
                .param("parts", "")
                .param("teamNumbers", "")
                .param("activityStatuses", "")
                .param("page", Int.MAX_VALUE.toString()),
        ).andExpect(status().isOk)
        val request = mockingDetails(service).invocations.single().arguments.single() as MemberManagementRequest
        assertThat(request.parts).isNullOrEmpty()
        assertThat(request.teamNumbers).isNullOrEmpty()
        assertThat(request.activityStatuses).isNullOrEmpty()
        assertThat(request.page).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `목록 조회 권한만으로 공통 선택지 조회 권한을 대신하지 않는다`() {
        mvc.perform(authenticated("read:member")).andExpect(status().isOk)
        mvc.perform(authenticatedRequest("/v3/members/parts", "read:member")).andExpect(status().isForbidden)
        mvc.perform(authenticatedRequest("/v3/cohorts/current/teams", "read:member")).andExpect(status().isForbidden)
        verifyNoInteractions(cohorts)
    }

    @Test
    fun `잘못된 페이지 크기와 필터를 400으로 반환한다`() {
        listOf("page" to "0", "size" to "101", "parts" to "MASTER", "parts" to "WEB,", "parts" to " ", "activityStatuses" to "WITHDRAWN", "activityStatuses" to "NORMAL,", "teamNumbers" to "-1", "teamNumbers" to "0,", "teamNumbers" to "not-a-number", "approvalStatus" to "UNKNOWN", "graduationStatuses" to "UNKNOWN", "page" to "not-a-number").forEach { (key, value) ->
            mvc.perform(authenticated("read:member").param(key, value))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("GLOBAL-400-01"))
        }
        verifyNoInteractions(service)
    }

    private fun authenticated(authority: String): MockHttpServletRequestBuilder = authenticatedRequest(PATH, authority)

    private fun authenticatedRequest(
        path: String,
        vararg authorities: String,
    ): MockHttpServletRequestBuilder =
        get(path).requestAttr(
            RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
            SecurityContextImpl(UsernamePasswordAuthenticationToken("operator", null, authorities.map(::SimpleGrantedAuthority))),
        )

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import(MemberManagementController::class, MemberPartController::class, CohortController::class, GlobalExceptionHandler::class)
    class Config {
        @Bean
        fun service(): MemberManagementQueryService = mock(MemberManagementQueryService::class.java)

        @Bean
        fun commands(): MemberManagementCommandService = mock(MemberManagementCommandService::class.java)

        @Bean
        fun cohorts(): CohortQueryService = mock(CohortQueryService::class.java)

        @Bean
        fun cohortCommands(): CohortCommandService = mock(CohortCommandService::class.java)

        @Bean
        fun security(http: HttpSecurity): SecurityFilterChain =
            http
                .csrf { it.disable() }
                .securityContext { it.securityContextRepository(RequestAttributeSecurityContextRepository()) }
                // 운영 SecurityConfig처럼 v3 URL은 permitAll. 컨트롤러의 read:member가 실제로 막는지 확인한다.
                .authorizeHttpRequests { it.anyRequest().permitAll() }
                .build()
    }

    companion object {
        private const val PATH = "/v3/members/overview"
    }
}

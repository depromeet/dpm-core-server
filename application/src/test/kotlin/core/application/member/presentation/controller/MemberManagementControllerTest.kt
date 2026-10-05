package core.application.member.presentation.controller

import core.application.common.exception.GlobalExceptionHandler
import core.application.member.application.service.MemberManagementQueryService
import core.application.member.presentation.request.MemberManagementRequest
import core.application.member.presentation.response.MemberManagementResponse
import core.application.member.presentation.response.MemberOverviewResponse
import core.domain.member.enums.MemberStatus
import io.swagger.v3.core.converter.ModelConverters
import jakarta.servlet.Filter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
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

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc =
            MockMvcBuilders.webAppContextSetup(context)
                .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(context.getBean("springSecurityFilterChain", Filter::class.java))
                .build()
        clearInvocations(service)
    }

    @Test
    fun `익명 및 조회 권한 없는 요청을 거절하고 서비스 호출을 막는다`() {
        mvc.perform(get(PATH)).andExpect(status().isForbidden)
        mvc.perform(authenticated("read:attendance")).andExpect(status().isForbidden)
        verifyNoInteractions(service)
    }

    @Test
    fun `조회 권한이 있으면 기존 응답 형식을 사용하고 중복 미평가 null을 반환한다`() {
        val member = MemberManagementResponse.MemberSummary(1, 19, "성휘", "signup@example.com", "SERVER", "DEEPER", 1, MemberStatus.ACTIVE, false, null, null, null)
        val response = MemberManagementResponse(19, MemberManagementResponse.Summary(1, 1, 0, 0, 0, 0, 0), 1, 1, 20, null, listOf(member))
        `when`(service.getOverview(MemberManagementRequest())).thenReturn(response)
        val result =
            mvc.perform(authenticated("read:member"))
                .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
            .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.members[0].signupEmail").value("signup@example.com"))
                .andReturn()
        assertThat(result.response.contentAsString).contains("\"duplicateSuspected\":null")
        assertThat(result.response.contentAsString).doesNotContain("\"email\":")
    }

    @Test
    fun `v1과 v3의 회원 항목은 Swagger에서 서로 다른 모델로 노출된다`() {
        val legacy = ModelConverters.getInstance().readAll(MemberOverviewResponse::class.java)
        val current = ModelConverters.getInstance().readAll(MemberManagementResponse::class.java)
        assertThat(legacy).containsKey("MemberSummary")
        assertThat(current).containsKeys("MemberManagementSummary", "MemberManagementTotals")
        assertThat(current).doesNotContainKey("MemberSummary")
        assertThat(current["MemberManagementSummary"]!!.properties).containsKey("signupEmail")
        assertThat(legacy["MemberSummary"]!!.properties).doesNotContainKey("signupEmail")
    }

    @Test
    fun `잘못된 페이지 크기와 필터를 400으로 반환한다`() {
        listOf("page" to "0", "size" to "101", "part" to "MASTER", "status" to "WITHDRAWN", "teamNumber" to "-1", "approvalStatus" to "UNKNOWN", "graduationStatuses" to "UNKNOWN", "page" to "not-a-number").forEach { (key, value) ->
            mvc.perform(authenticated("read:member").param(key, value))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("GLOBAL-400-01"))
        }
        verifyNoInteractions(service)
    }

    private fun authenticated(authority: String): MockHttpServletRequestBuilder =
        get(PATH).requestAttr(
            RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
            SecurityContextImpl(UsernamePasswordAuthenticationToken("operator", null, listOf(SimpleGrantedAuthority(authority)))),
        )

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import(MemberManagementController::class, GlobalExceptionHandler::class)
    class Config {
        @Bean
        fun service(): MemberManagementQueryService = mock(MemberManagementQueryService::class.java)

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

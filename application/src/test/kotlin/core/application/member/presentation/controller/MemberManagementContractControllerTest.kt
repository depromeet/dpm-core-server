package core.application.member.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import core.application.common.configuration.SwaggerConfig
import core.application.common.exception.GlobalExceptionHandler
import core.application.member.application.service.MemberAdmissionService
import core.application.member.application.service.MemberBadgeService
import core.application.member.application.service.MemberDeletionService
import core.application.member.presentation.response.MemberBadgeCard
import core.application.member.presentation.response.MemberBadgeResponse
import core.application.member.presentation.response.MemberBadgesResponse
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import jakarta.servlet.Filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springdoc.core.configuration.SpringDocConfiguration
import org.springdoc.core.properties.SpringDocConfigProperties
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringJUnitConfig(MemberManagementContractControllerTest.Config::class)
@WebAppConfiguration
class MemberManagementContractControllerTest {
    @Autowired lateinit var context: WebApplicationContext

    @Autowired lateinit var mapper: ObjectMapper

    @Autowired lateinit var admissionService: MemberAdmissionService

    @Autowired lateinit var badgeService: MemberBadgeService

    @Autowired lateinit var members: MemberPersistencePort

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        reset(admissionService, badgeService)
        `when`(badgeService.getBadges()).thenReturn(MemberBadgesResponse(19, listOf(MemberBadgeResponse(MemberBadgeCard.PENDING, true, 3))))
        `when`(badgeService.acknowledge(MemberBadgeCard.PENDING, 19, 3)).thenReturn(MemberBadgeResponse(MemberBadgeCard.PENDING, false, 3))
        reset(members)
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, setOf(19))))
        `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet()), MemberApprovalTarget(2, MemberStatus.INACTIVE, setOf(18))))
        mvc =
            MockMvcBuilders.webAppContextSetup(context)
                .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(context.getBean("springSecurityFilterChain", Filter::class.java))
                .build()
    }

    @Test
    fun `NEW 조회와 확인은 서비스를 호출하고 상태를 반환한다`() {
        mvc.perform(authenticated(get("/v3/members/badges"), "read:member"))
            .andExpect(status().isOk).andExpect(jsonPath("$.data.cohortId").value(19))
            .andExpect(jsonPath("$.data.cards[0].hasNew").value(true))
        mvc.perform(authenticated(json(post(BADGE_PATH), BADGE_BODY), "read:member"))
            .andExpect(status().isOk).andExpect(jsonPath("$.data.hasNew").value(false))
        verify(badgeService).acknowledge(MemberBadgeCard.PENDING, 19, 3)
    }

    @Test
    fun `통합 권한이 있으면 서비스에 두 ID를 전달한다`() {
        mvc.perform(authenticated(json(post("/v3/members/merge"), MERGE_BODY), "create:member", "delete:member"))
            .andExpect(status().isOk)
        verify(context.getBean(core.application.member.application.service.MemberMergeService::class.java)).mergeAndApprove(1, 2)
    }

    @Test
    fun `익명은 모든 계약 API에 접근할 수 없다`() {
        endpoints().forEach { request ->
            val result = mvc.perform(request).andReturn()
            assertTrue(result.response.status in listOf(401, 403))
        }
    }

    @Test
    fun `관리 권한과 본인 재신청 권한을 구분한다`() {
        endpoints().filterNot { it.buildRequest(context.servletContext!!).requestURI?.endsWith("/reapplication") == true }.forEach { request ->
            mvc.perform(authenticated(request)).andExpect(status().isForbidden)
        }
        listOf("create:member", "delete:member").forEach { authority ->
            mvc.perform(authenticated(json(post("/v3/members/merge"), MERGE_BODY), authority))
                .andExpect(status().isForbidden)
        }
        mvc.perform(authenticated(post("/v3/members/me/reapplication")))
            .andExpect(status().isOk)
        verify(admissionService).reapply(1)
    }

    @Test
    fun `가입 반려는 생성 권한을 확인하고 경로 식별자를 전달한다`() {
        mvc.perform(authenticated(post("/v3/members/42/rejection"), "create:member"))
            .andExpect(status().isOk)
        verify(admissionService).reject(42)
        mvc.perform(authenticated(post("/v3/members/wrong/rejection"), "create:member"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `재신청은 요청에 타인 ID가 있어도 인증된 본인 ID만 사용한다`() {
        mvc.perform(authenticated(json(post("/v3/members/me/reapplication"), """{"memberId":42}""")))
            .andExpect(status().isOk)
        verify(admissionService).reapply(1)
    }

    @Test
    fun `통합 식별자와 일괄 삭제 목록을 검증한다`() {
        listOf(
            """{"retainedMemberId":1,"sourceMemberId":1}""",
            """{"retainedMemberId":null,"sourceMemberId":2}""",
            """{"retainedMemberId":0,"sourceMemberId":2}""",
            """{"retainedMemberId":"1","sourceMemberId":2}""",
        ).forEach { body ->
            mvc.perform(authenticated(json(post("/v3/members/merge"), body), "create:member", "delete:member"))
                .andExpect(status().isBadRequest)
        }
        listOf("[]", "[null]", "[0]", "[1,1]", "[1.5]", "[\"1\"]").forEach { ids ->
            mvc.perform(authenticated(json(delete("/v3/members/bulk"), """{"memberIds":$ids}"""), "delete:member"))
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `NEW 확인에는 조회한 기수와 음수가 아닌 버전이 필요하다`() {
        listOf(
            """{"cohortId":19}""",
            """{"cohortId":0,"version":1}""",
            """{"cohortId":19,"version":-1}""",
            """{"cohortId":19,"version":null}""",
            """{"cohortId":19,"version":"1"}""",
        ).forEach { body ->
            mvc.perform(authenticated(json(post(BADGE_PATH), body), "read:member"))
                .andExpect(status().isBadRequest)
        }
        mvc.perform(authenticated(json(post("/v3/members/badges/UNKNOWN/acknowledgement"), BADGE_BODY), "read:member"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `생성된 OpenAPI에 실제 경로와 정상 응답이 포함되고 미구현 응답은 없다`() {
        val result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk).andReturn()
        val document = mapper.readTree(result.response.contentAsString)
        val expected =
            mapOf(
                "/v3/members/merge" to "post",
                "/v3/members/{memberId}/rejection" to "post",
                "/v3/members/me/reapplication" to "post",
                "/v3/members/{memberId}" to "delete",
                "/v3/members/bulk" to "delete",
                "/v3/members/badges" to "get",
                "/v3/members/badges/{card}/acknowledgement" to "post",
            )
        expected.forEach { (path, method) ->
            val operation = document["paths"][path][method]
            assertFalse(operation["responses"].has("501"), path)
            assertTrue(operation["responses"].has("200"), path)
        }
        assertFalse(document["security"].isEmpty)
        val schemas = document["components"]["schemas"]
        assertTrue(schemas["MemberBadgesResponse"]["properties"].has("cohortId"))
        assertTrue(schemas["MemberBadgeResponse"]["properties"].has("version"))
        assertEquals(
            listOf("PENDING", "INCOMPLETE", "AT_RISK"),
            schemas["MemberBadgeResponse"]["properties"]["card"]["enum"].map { it.asText() },
        )
        assertFalse(schemas["MemberMergeRequest"]["properties"].has("distinct"))
        assertFalse(document["paths"].has("/v3/members/{memberId}/hard-delete"))
        assertFalse(document["paths"].has("/v3/members/hard-delete/bulk"))
        assertTrue(document["paths"]["/v3/members/me/reapplication"]["post"].path("parameters").isMissingNode)
    }

    @Test
    fun `단건 일괄 삭제는 성공 계약과 실제 서비스 호출을 제공한다`() {
        mvc.perform(authenticated(delete("/v3/members/1"), "delete:member", actorMemberId = 3))
            .andExpect(status().isOk).andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
        verify(members).softDeleteMembers(listOf(1))
        mvc.perform(authenticated(json(delete("/v3/members/bulk"), """{"memberIds":[2,1]}"""), "delete:member", actorMemberId = 3))
            .andExpect(status().isOk).andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
        verify(members).softDeleteMembers(listOf(1, 2))
    }

    @Test
    fun `인증된 본인 삭제와 본인이 포함된 일괄 삭제는 모두 거절한다`() {
        mvc.perform(authenticated(json(delete("/v3/members/1"), """{"actorMemberId":3}"""), "delete:member"))
            .andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("MEMBER-403-03"))
        mvc.perform(authenticated(json(delete("/v3/members/bulk"), """{"memberIds":[2,1]}"""), "delete:member"))
            .andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("MEMBER-403-03"))
        mvc.perform(authenticated(json(delete("/v3/members/bulk"), """{"memberIds":[2,1],"actorMemberId":3}"""), "delete:member"))
            .andExpect(status().isBadRequest)
        verifyNoInteractions(members)
    }

    @Test
    fun `없는 삭제 회원은 404 잘못된 단건 ID는 400을 반환한다`() {
        listOf("0", "-1", "wrong").forEach {
            mvc.perform(authenticated(delete("/v3/members/$it"), "delete:member")).andExpect(status().isBadRequest)
        }
        mvc.perform(authenticated(delete("/v3/members/999"), "delete:member"))
            .andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("MEMBER-404-01"))
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, emptySet(), true)))
        mvc.perform(authenticated(delete("/v3/members/1"), "delete:member", actorMemberId = 3)).andExpect(status().isNotFound)
    }

    private fun endpoints(): List<MockHttpServletRequestBuilder> = stubEndpoints() + listOf(post("/v3/members/1/rejection"), post("/v3/members/me/reapplication"), json(post("/v3/members/merge"), MERGE_BODY), delete("/v3/members/1"), json(delete("/v3/members/bulk"), """{"memberIds":[1,2]}"""))

    private fun stubEndpoints(): List<MockHttpServletRequestBuilder> =
        listOf(
            get("/v3/members/badges"),
            json(post(BADGE_PATH), BADGE_BODY),
        )

    private fun json(
        request: MockHttpServletRequestBuilder,
        body: String,
    ): MockHttpServletRequestBuilder = request.contentType(MediaType.APPLICATION_JSON).content(body)

    private fun authenticated(
        request: MockHttpServletRequestBuilder,
        vararg authorities: String,
        actorMemberId: Long = 1,
    ): MockHttpServletRequestBuilder =
        request.requestAttr(
            RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
            SecurityContextImpl(UsernamePasswordAuthenticationToken(actorMemberId.toString(), null, authorities.map(::SimpleGrantedAuthority))),
        )

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import(
        MemberAdmissionController::class,
        MemberDeletionController::class,
        MemberDeletionService::class,
        MemberBadgeController::class,
        GlobalExceptionHandler::class,
        SwaggerConfig::class,
        SpringDocConfiguration::class,
        SpringDocConfigProperties::class,
        SpringDocWebMvcConfiguration::class,
    )
    @ImportAutoConfiguration(JacksonAutoConfiguration::class)
    class Config : WebMvcConfigurer {
        @Bean fun badgeService(): MemberBadgeService = mock(MemberBadgeService::class.java)

        @Bean
        fun mergeService(): core.application.member.application.service.MemberMergeService = mock(core.application.member.application.service.MemberMergeService::class.java)

        @Bean
        fun admissionService(): MemberAdmissionService = mock(MemberAdmissionService::class.java)

        @Bean fun members(): MemberPersistencePort = mock(MemberPersistencePort::class.java)

        override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
            resolvers.add(CurrentMemberIdArgumentResolver())
        }

        @Bean
        fun security(http: HttpSecurity): SecurityFilterChain =
            http.csrf { it.disable() }
                .securityContext { it.securityContextRepository(RequestAttributeSecurityContextRepository()) }
                .authorizeHttpRequests { it.anyRequest().permitAll() }
                .build()
    }

    private companion object {
        const val MERGE_BODY = """{"retainedMemberId":1,"sourceMemberId":2}"""
        const val BADGE_PATH = "/v3/members/badges/PENDING/acknowledgement"
        const val BADGE_BODY = """{"cohortId":19,"version":3}"""
    }
}

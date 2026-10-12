package core.application.member.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import core.application.common.configuration.SwaggerConfig
import core.application.common.exception.GlobalExceptionHandler
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import jakarta.servlet.Filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc =
            MockMvcBuilders.webAppContextSetup(context)
                .addFilters<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(context.getBean("springSecurityFilterChain", Filter::class.java))
                .build()
    }

    @Test
    fun `권한이 있어도 명세 전용 API는 성공을 반환하지 않는다`() {
        endpoints().forEach { request ->
            mvc.perform(authenticated(request, "create:member", "delete:member", "read:member"))
                .andExpect(status().isNotImplemented)
                .andExpect(jsonPath("$.code").value("MEMBER-501-01"))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
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
            .andExpect(status().isNotImplemented)
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
    fun `생성된 OpenAPI에 경로와 정상 응답 및 미구현 응답이 모두 포함된다`() {
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
            assertTrue(operation["responses"].has("501"), path)
            assertTrue(operation["responses"]["200"].has("content"), path)
            val errorSchema = operation["responses"]["501"].path("content").elements().asSequence().firstOrNull()?.path("schema")
            assertTrue(errorSchema != null && !errorSchema.isMissingNode, "$path: ${operation["responses"]["501"]}")
            assertFalse(errorSchema.toString().contains("MemberBadge"), "$path: $errorSchema")
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

    private fun endpoints(): List<MockHttpServletRequestBuilder> =
        listOf(
            json(post("/v3/members/merge"), MERGE_BODY),
            post("/v3/members/1/rejection"),
            post("/v3/members/me/reapplication"),
            delete("/v3/members/1"),
            json(delete("/v3/members/bulk"), """{"memberIds":[1,2]}"""),
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
    ): MockHttpServletRequestBuilder =
        request.requestAttr(
            RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME,
            SecurityContextImpl(UsernamePasswordAuthenticationToken("1", null, authorities.map(::SimpleGrantedAuthority))),
        )

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import(
        MemberAdmissionController::class,
        MemberDeletionController::class,
        MemberBadgeController::class,
        GlobalExceptionHandler::class,
        SwaggerConfig::class,
        SpringDocConfiguration::class,
        SpringDocConfigProperties::class,
        SpringDocWebMvcConfiguration::class,
    )
    @ImportAutoConfiguration(JacksonAutoConfiguration::class)
    class Config : WebMvcConfigurer {
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

package core.application.notification.presentation.controller

import core.application.common.exception.GlobalExceptionHandler
import core.application.notification.application.service.NotificationCommandService
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.aop.framework.ProxyFactory
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/** 메서드 보안을 실제로 거쳐 디퍼 권한으로는 푸시 테스트 발송이 막히고 토큰 등록은 유지되는지 확인한다. */
class NotificationControllerAuthorizationTest {
    private val service: NotificationCommandService = mock(NotificationCommandService::class.java)

    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(
                ProxyFactory(NotificationController(service))
                    .apply {
                        isProxyTargetClass = true
                        addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize())
                    }.proxy,
            ).setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(CurrentMemberIdArgumentResolver())
            .build()

    @AfterEach
    fun clear() {
        SecurityContextHolder.clearContext()
        reset(service)
    }

    @Test
    fun `디퍼 권한으로는 푸시 테스트 발송과 유형 조회를 할 수 없다`() {
        loginAs(1L, *DEEPER_AUTHORITIES)

        mockMvc.perform(get("/v1/notifications/types"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("GLOBAL-403-01"))
        mockMvc.perform(json(post("/v1/notifications/custom-test"), """{"targetMemberId":2,"title":"t","message":"m"}"""))
            .andExpect(status().isForbidden)
        mockMvc.perform(
            json(
                post("/v1/notifications/custom-test/by-tag"),
                """{"tags":[{"cohortId":4,"authorityId":2}],"title":"t","message":"m"}""",
            ),
        ).andExpect(status().isForbidden)
        mockMvc.perform(json(post("/v1/notifications/type-test"), """{"targetMemberId":2,"notificationMessageType":"SESSION_START_SOON"}"""))
            .andExpect(status().isForbidden)
        mockMvc.perform(
            json(
                post("/v1/notifications/type-test/by-tag"),
                """{"tags":[{"cohortId":4,"authorityId":2}],"notificationMessageType":"SESSION_START_SOON"}""",
            ),
        ).andExpect(status().isForbidden)
        verifyNoInteractions(service)
    }

    @Test
    fun `운영진도 코어 권한이 없으면 푸시 테스트 유형을 조회할 수 없고 코어는 조회한다`() {
        loginAs(1L, "read:member", "update:member", "update:attendance")
        mockMvc.perform(get("/v1/notifications/types")).andExpect(status().isForbidden)

        loginAs(1L, "update:authorization")
        mockMvc.perform(get("/v1/notifications/types")).andExpect(status().isOk)
    }

    @Test
    fun `디퍼 앱의 푸시 토큰 등록은 기존 권한으로 계속 동작한다`() {
        loginAs(1L, *DEEPER_AUTHORITIES)
        mockMvc.perform(json(post("/v1/notifications/tokens"), """{"token":"ExponentPushToken[abc]"}"""))
            .andExpect(status().isOk)
    }

    private fun json(
        request: org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder,
        body: String,
    ) = request.contentType(MediaType.APPLICATION_JSON).content(body)

    private fun loginAs(
        memberId: Long,
        vararg authorities: String,
    ) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(memberId.toString(), null, authorities.map(::SimpleGrantedAuthority))
    }

    private companion object {
        val DEEPER_AUTHORITIES = arrayOf("read:member", "read:attendance", "create:attendance", "read:session")
    }
}

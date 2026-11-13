package core.application.announcement.presentation.controller

import core.application.announcement.application.service.AnnouncementCommandService
import core.application.announcement.application.service.AnnouncementQueryService
import core.application.common.exception.GlobalExceptionHandler
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.aop.framework.ProxyFactory
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/** 메서드 보안을 실제로 거쳐 비로그인·디퍼는 공지 관리와 회원 현황 조회를 할 수 없는지 확인한다. */
class AnnouncementControllerAuthorizationTest {
    private val commandService: AnnouncementCommandService = mock(AnnouncementCommandService::class.java)
    private val queryService: AnnouncementQueryService = mock(AnnouncementQueryService::class.java)

    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(
                withPreAuthorize(AnnouncementCommandController(commandService)),
                withPreAuthorize(AnnouncementQueryController(queryService)),
            ).setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(CurrentMemberIdArgumentResolver())
            .build()

    @AfterEach
    fun clear() {
        SecurityContextHolder.clearContext()
        reset(commandService, queryService)
    }

    @Test
    fun `비로그인 요청은 회원 현황 조회 리마인드 푸시 삭제를 할 수 없다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))

        // 로그인 회원 ID 인자가 없는 API는 권한 단계에서 403이다.
        listOf(
            get("/v1/announcements/1/mark-as-read/members"),
            get("/v1/announcements/1/assignment-status"),
            post("/v1/announcements/1/remind-notification"),
        ).forEach { request ->
            mockMvc.perform(request).andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("GLOBAL-403-01"))
        }
        // 로그인 회원 ID 인자가 있는 API는 인자 단계에서 401이다.
        mockMvc.perform(delete("/v1/announcements/1")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/v1/announcements/1")).andExpect(status().isUnauthorized)
        verifyNoInteractions(commandService, queryService)
    }

    @Test
    fun `디퍼 권한으로는 공지를 읽음 처리할 수 있지만 관리와 회원 현황 조회는 할 수 없다`() {
        loginAs(1L, *DEEPER_AUTHORITIES)

        listOf(
            get("/v1/announcements/1/mark-as-read/members"),
            get("/v1/announcements/1/assignment-status"),
            post("/v1/announcements/1/remind-notification"),
            delete("/v1/announcements/1"),
        ).forEach { request ->
            mockMvc.perform(request).andExpect(status().isForbidden)
        }
        verifyNoInteractions(commandService, queryService)

        mockMvc.perform(post("/v1/announcements/1/mark-as-read")).andExpect(status().isOk)
        verify(commandService).markAsRead(memberId = MemberId(1), announcementId = AnnouncementId(1))
    }

    @Test
    fun `운영진은 공지를 삭제하고 리마인드 푸시를 보낼 수 있다`() {
        loginAs(1L, "update:member")
        mockMvc.perform(delete("/v1/announcements/1")).andExpect(status().isOk)
        mockMvc.perform(post("/v1/announcements/1/remind-notification")).andExpect(status().isOk)
        verify(commandService).delete(AnnouncementId(1))
        verify(commandService).remindNotification(AnnouncementId(1))
    }

    private fun withPreAuthorize(controller: Any): Any =
        ProxyFactory(controller)
            .apply {
                isProxyTargetClass = true
                addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize())
            }.proxy

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

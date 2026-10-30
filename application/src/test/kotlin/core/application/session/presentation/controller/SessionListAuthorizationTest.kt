package core.application.session.presentation.controller

import com.fasterxml.jackson.databind.SerializationFeature
import core.application.common.exception.GlobalExceptionHandler
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import core.application.session.application.service.SessionQueryService
import core.application.session.presentation.response.NextSessionHomeResponse
import core.application.sessionFeedback.application.service.SessionFeedbackFormQueryService
import core.application.sessionFeedback.application.service.SessionFeedbackListQueryService
import core.application.sessionFeedback.presentation.response.SessionListFeedbackResponse
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import core.domain.session.aggregate.Session
import core.domain.session.enums.NextSessionHomeStatus
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
import core.domain.sessionFeedback.enums.SessionFeedbackStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.aop.framework.ProxyFactory
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/** 실제 메서드 보안과 예외 응답을 검사한다. 토큰에서 현재 기수 권한을 구하는 부분은 별도 테스트에서 확인한다. */
class SessionListAuthorizationTest {
    private val sessions = mock(SessionQueryService::class.java)
    private val feedback = mock(SessionFeedbackListQueryService::class.java)
    private val forms = mock(SessionFeedbackFormQueryService::class.java)
    private val controller =
        ProxyFactory(SessionQueryController(sessions, forms, feedback, Clock.systemUTC()))
            .apply {
                isProxyTargetClass = true
                addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize())
            }.proxy
    private val mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(CurrentMemberIdArgumentResolver())
            .setMessageConverters(
                MappingJackson2HttpMessageConverter(
                    Jackson2ObjectMapperBuilder.json()
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build(),
                ),
            ).build()

    @AfterEach
    fun clearAuthentication() = SecurityContextHolder.clearContext()

    @Test
    fun `비로그인 요청에는 세션과 피드백을 조회하지 않고 403을 반환한다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken("anonymous", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))

        assertForbidden()
    }

    @Test
    fun `로그인했어도 현재 세션 조회 권한이 없으면 목록을 반환하지 않는다`() {
        loginAs()

        assertForbidden()
    }

    @Test
    fun `다른 조회 권한으로 세션 목록에 접근할 수 없다`() {
        loginAs("read:attendance")

        assertForbidden()
    }

    @Test
    fun `세션 조회 권한이 있으면 기존 목록 형식과 본인의 피드백 정보를 유지한다`() {
        loginAs("read:session")
        val date = Instant.parse("2026-10-25T07:30:00Z")
        val session =
            Session(
                id = SessionId(52),
                cohortId = CohortId(19),
                date = date,
                week = 1,
                place = "세션 장소",
                eventName = "19기 세션",
                isOnline = false,
                attendancePolicy = AttendancePolicy(date, date.plusSeconds(600), date.plusSeconds(1200), "1234"),
            )
        val list = listOf(session)
        given(sessions.getAllCurrentCohortSessions()).willReturn(list)
        given(feedback.buildFor(list, MemberId(7))).willReturn(
            mapOf(52L to SessionListFeedbackResponse(SessionFeedbackStatus.IN_PROGRESS, LocalDateTime.parse("2026-10-30T18:00:00"), true)),
        )

        mvc.perform(get("/v1/sessions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
            .andExpect(jsonPath("$.data.sessions[0].id").value(52))
            .andExpect(jsonPath("$.data.sessions[0].name").value("19기 세션"))
            .andExpect(jsonPath("$.data.sessions[0].date").value("2026-10-25T16:30:00"))
            .andExpect(jsonPath("$.data.sessions[0].feedback.canSubmit").value(true))
            .andExpect(jsonPath("$.data.sessions[0].feedback.status").value("IN_PROGRESS"))
        verify(feedback).buildFor(list, MemberId(7))
    }

    @Test
    fun `기수가 끝난 회원도 홈의 기수 종료 안내는 조회할 수 있다`() {
        loginAs()
        given(sessions.getNextSessionHome(MemberId(7))).willReturn(
            NextSessionHomeResponse(NextSessionHomeStatus.COHORT_ENDED, "18", null),
        )

        mvc.perform(get("/v1/sessions/next"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("COHORT_ENDED"))
            .andExpect(jsonPath("$.data.cohortValue").value("18"))
        verifyNoInteractions(feedback, forms)
    }

    private fun assertForbidden() {
        mvc.perform(get("/v1/sessions"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("GLOBAL-403-01"))
            .andExpect(jsonPath("$.data.sessions").doesNotExist())
        verifyNoInteractions(sessions, feedback, forms)
    }

    private fun loginAs(vararg authorities: String) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("7", null, authorities.map(::SimpleGrantedAuthority))
    }
}

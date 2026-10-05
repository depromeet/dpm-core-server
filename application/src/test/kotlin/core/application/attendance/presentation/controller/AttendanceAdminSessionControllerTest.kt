package core.application.attendance.presentation.controller

import com.fasterxml.jackson.databind.SerializationFeature
import core.application.attendance.application.service.AbsenceReasonCommandService
import core.application.attendance.application.service.AbsenceReasonQueryService
import core.application.attendance.application.service.AttendanceCommandService
import core.application.attendance.application.service.AttendanceQueryService
import core.application.attendance.presentation.response.SessionRosterMemberResponse
import core.application.attendance.presentation.response.SessionRosterResponse
import core.application.cohort.application.service.CohortCommandService
import core.application.cohort.application.service.CohortQueryService
import core.application.cohort.presentation.controller.CohortController
import core.application.common.exception.GlobalExceptionHandler
import core.application.member.presentation.controller.MemberPartController
import core.application.security.resolver.CurrentMemberIdArgumentResolver
import core.application.session.application.service.SessionQueryService
import core.application.session.presentation.controller.SessionQueryController
import core.application.support.FakeCohortPersistencePort
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.query.CohortTeamQueryModel
import core.domain.member.vo.MemberId
import core.domain.session.enums.SessionAttendanceStatus
import core.domain.session.port.inbound.query.SessionSelectorQueryModel
import core.domain.session.port.inbound.query.SessionWeekQueryModel
import core.domain.session.vo.SessionId
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.aop.framework.ProxyFactory
import org.springframework.http.MediaType
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/** 운영진 세션 화면 API 의 경로 매칭, 요청 상태 검증, 메서드 권한을 확인한다. 보안 필터 체인은 띄우지 않는다. */
class AttendanceAdminSessionControllerTest {
    private val queryService: AttendanceQueryService = mock(AttendanceQueryService::class.java)
    private val commandService: AttendanceCommandService = mock(AttendanceCommandService::class.java)
    private val sessionQueryService: SessionQueryService = mock(SessionQueryService::class.java)
    private val cohorts = FakeCohortPersistencePort()

    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(
                withPreAuthorize(AttendanceQueryController(queryService, mock(AbsenceReasonQueryService::class.java))),
                withPreAuthorize(
                    AttendanceCommandController(commandService, mock(AbsenceReasonCommandService::class.java), Clock.systemUTC()),
                ),
                withPreAuthorize(SessionQueryController(sessionQueryService)),
                withPreAuthorize(CohortController(CohortQueryService(cohorts), mock(CohortCommandService::class.java))),
                withPreAuthorize(MemberPartController()),
            ).setControllerAdvice(GlobalExceptionHandler())
            // Spring Boot 기본값처럼 날짜를 ISO 문자열로 쓴다(설정 파일에 jackson 재정의 없음).
            .setMessageConverters(
                MappingJackson2HttpMessageConverter(
                    Jackson2ObjectMapperBuilder.json().featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
                ),
            )
            .setCustomArgumentResolvers(CurrentMemberIdArgumentResolver())
            .build()

    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    @Test
    fun `운영진 전체 명단은 출석 수정 권한이 있어야 한다`() {
        loginAs(1L, "read:attendance", "create:attendance")
        mockMvc
            .perform(get("/v3/sessions/3/attendances"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("GLOBAL-403-01"))
        verifyNoInteractions(queryService)

        loginAs(1L, "update:attendance")
        mockMvc.perform(get("/v3/sessions/3/attendances")).andExpect(status().isOk)
        verify(queryService).getSessionRoster(SessionId(3), MemberId(1))
    }

    @Test
    fun `수정 상태는 다섯 상태만 받고 없앤 조퇴나 모르는 값은 단건과 일괄 모두 400 이다`() {
        loginAs(1L, "update:attendance")

        listOf("EARLY_LEAVE", "UNKNOWN", "present", "").forEach { invalid ->
            mockMvc
                .perform(patchJson("/v3/sessions/3/attendances/7", """{"attendanceStatus":"$invalid"}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("GLOBAL-400-01"))
            mockMvc
                .perform(patchJson("/v3/sessions/3/attendances/bulk", """{"attendanceStatus":"$invalid","memberIds":[1]}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("GLOBAL-400-01"))
        }
        verifyNoInteractions(commandService)

        mockMvc
            .perform(patchJson("/v3/sessions/3/attendances/7", """{"attendanceStatus":"LATE"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
        verify(commandService).updateAttendanceStatus(AttendanceStatusUpdateCommand(SessionId(3), MemberId(7), AttendanceStatus.LATE))
    }

    @Test
    fun `명단 응답은 null 필드도 키로 주고 날짜는 ISO 문자열이다`() {
        loginAs(1L, "update:attendance")
        given(queryService.getSessionRoster(SessionId(3), MemberId(1))).willReturn(
            SessionRosterResponse(
                members =
                    listOf(
                        SessionRosterMemberResponse(1, "신민철", 1, false, "SERVER", "PRESENT", LocalDateTime.parse("2025-08-02T13:55:12"), false, null),
                        SessionRosterMemberResponse(2, "이정호", null, true, "WEB", "EXCUSED_ABSENT", null, true, "병원 진료"),
                    ),
                myTeamNumber = null,
            ),
        )

        mockMvc
            .perform(get("/v3/sessions/3/attendances"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
            .andExpect(jsonPath("$.data.members[0].attendedAt").value("2025-08-02T13:55:12"))
            .andExpect(jsonPath("$.data.members[1].isManuallyUpdated").value(true))
            // jsonPath exists 는 null 을 없음으로 보므로 키 존재로 확인한다
            .andExpect(jsonPath("$.data", hasKey("myTeamNumber")))
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.members[1]", hasKey("teamNumber")))
            .andExpect(jsonPath("$.data.members[1]", hasKey("attendedAt")))
    }

    @Test
    fun `세션 선택은 v1 이 기존 세 필드 그대로이고 v3 가 확장 필드와 출석 인증 상태를 준다`() {
        loginAs(1L, "read:session")
        val date = Instant.parse("2025-08-02T04:00:00Z")
        given(sessionQueryService.getSessionWeeks()).willReturn(listOf(SessionWeekQueryModel(SessionId(5), 1, date)))
        given(sessionQueryService.getSessionSelector()).willReturn(
            listOf(SessionSelectorQueryModel(SessionId(5), 1, date, "OT", "디프만 오프라인 장소", false, SessionAttendanceStatus.IN_PROGRESS)),
        )

        mockMvc
            .perform(get("/v1/sessions/weeks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.sessions[0].date").value("2025-08-02T13:00:00"))
            .andExpect(jsonPath("$.data.sessions[0].*", hasSize<Any>(3)))
        mockMvc
            .perform(get("/v3/sessions/weeks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
            .andExpect(jsonPath("$.data.sessions[0].id").value(5))
            .andExpect(jsonPath("$.data.sessions[0].isOnline").value(false))
            .andExpect(jsonPath("$.data.sessions[0].attendanceStatus").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.data.sessions[0].*", hasSize<Any>(7)))
    }

    @Test
    fun `현재 기수 팀 목록은 출석 수정 권한으로 활성 기수(최댓값 아님)의 팀을 그대로 준다`() {
        val active = cohorts.save(Cohort(value = "17"))
        cohorts.activate(active.id!!)
        val higherInactive = cohorts.save(Cohort(value = "18"))
        cohorts.teams[active.id!!.value] = listOf(CohortTeamQueryModel(31, 1), CohortTeamQueryModel(37, 7))
        cohorts.teams[higherInactive.id!!.value] = listOf(CohortTeamQueryModel(41, 1))

        loginAs(1L, "read:attendance", "create:attendance")
        mockMvc
            .perform(get("/v3/cohorts/current/teams"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("GLOBAL-403-01"))

        loginAs(1L, "update:attendance")
        mockMvc
            .perform(get("/v3/cohorts/current/teams"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
            .andExpect(jsonPath("$.data.teams[*].id", contains(31, 37)))
            .andExpect(jsonPath("$.data.teams[*].number", contains(1, 7)))
    }

    @Test
    fun `파트 선택지는 출석 수정 권한으로 파트 정의 순서 뒤에 미지정을 붙여 준다`() {
        loginAs(1L, "read:attendance", "create:attendance")
        mockMvc
            .perform(get("/v3/members/parts"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("GLOBAL-403-01"))

        loginAs(1L, "update:attendance")
        mockMvc
            .perform(get("/v3/members/parts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GLOBAL-200-01"))
            .andExpect(jsonPath("$.data.parts", contains("WEB", "SERVER", "DESIGN", "IOS", "ANDROID", "UNASSIGNED")))
    }

    /** 운영과 같은 @PreAuthorize 검사만 붙인다. */
    private fun withPreAuthorize(controller: Any): Any =
        ProxyFactory(controller)
            .apply {
                isProxyTargetClass = true
                addAdvisor(AuthorizationManagerBeforeMethodInterceptor.preAuthorize())
            }.proxy

    private fun patchJson(
        path: String,
        body: String,
    ) = patch(path).contentType(MediaType.APPLICATION_JSON).content(body)

    private fun loginAs(
        memberId: Long,
        vararg authorities: String,
    ) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(memberId.toString(), null, authorities.map(::SimpleGrantedAuthority))
    }
}

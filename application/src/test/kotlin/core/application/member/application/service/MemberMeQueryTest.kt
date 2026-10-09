package core.application.member.application.service

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import core.application.attendance.application.service.AttendanceGraduationEvaluator
import core.application.cohort.application.service.CohortQueryService
import core.application.member.application.service.access.MemberAccessService
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.support.FakeAttendancePersistencePort
import core.application.support.FakeCohortPersistencePort
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberCohort
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.MemberStatus
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberCohortId
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.domain.team.vo.TeamNumber
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock

/** 마이페이지 출석 집계와 수료 상태. 집계 SQL(기록 없음 0, 삭제 기록 제외, 기수별 팀)은 MySQL 통합 테스트에서 검증한다. */
class MemberMeQueryTest {
    private val memberPersistencePort = mock(MemberPersistencePort::class.java)
    private val memberOAuthPersistencePort = mock(MemberOAuthPersistencePort::class.java)
    private val attendances = FakeAttendancePersistencePort()
    private val cohorts = FakeCohortPersistencePort()
    private val service = service(defaultTeamId = 0)

    @Test
    fun `반려 회원의 내 정보는 REJECTED 상태를 그대로 제공한다`() {
        val memberId = MemberId(1)
        given(memberPersistencePort.findById(memberId)).willReturn(
            Member(id = memberId, name = "홍길동", email = "member@example.com", signupEmail = "member@example.com", status = MemberStatus.REJECTED),
        )
        val response = service.memberMe(memberId, null)
        assertThat(response.status).isEqualTo("REJECTED")
        assertThat(response.isActiveCohort).isFalse()
    }

    private fun service(defaultTeamId: Int) =
        MemberQueryService(
            memberPersistencePort = memberPersistencePort,
            memberAccessService = mock(MemberAccessService::class.java),
            memberOAuthService = mock(MemberOAuthService::class.java),
            memberLoginEmailResolver =
                MemberLoginEmailResolver(
                    memberOAuthPersistencePort = memberOAuthPersistencePort,
                    memberCredentialPersistencePort = mock(MemberCredentialPersistencePort::class.java),
                ),
            cohortQueryUseCase = CohortQueryService(cohorts),
            attendancePersistencePort = attendances,
            attendanceGraduationEvaluator = AttendanceGraduationEvaluator(),
            defaultTeamId = defaultTeamId,
        )

    @Test
    fun `현재 기수 멤버는 그 기수 집계와 기존 수료 판정을 주고 멤버 상태와는 별개다`() {
        val current = cohorts.save(Cohort(value = "18", isActive = true)).id!!
        val cases =
            listOf(
                summary(present = 9) to "NORMAL",
                summary(present = 6, late = 2, onlineAbsent = 2) to "AT_RISK",
                summary(present = 5, offlineAbsent = 3) to "IMPOSSIBLE",
            )

        cases.forEachIndexed { index, (summary, expected) ->
            val memberId = MemberId(index + 1L)
            givenMember(memberId, cohort(1L, memberId, current, "18"))
            attendances.memberDetails[memberId.value to current.value] = detail(memberId, team = 2, summary = summary)

            val response = service.memberMe(memberId, null)

            assertThat(response.isActiveCohort).isTrue()
            assertThat(response.attendanceStatus).isEqualTo(expected)
            assertThat(response.status).isEqualTo("ACTIVE")
            assertThat(response.teamNumber).isEqualTo(TeamNumber(2))
            assertThat(listOf(response.presentCount, response.lateCount, response.excusedAbsentCount, response.absentCount))
                .containsExactly(summary.presentCount, summary.lateCount, summary.excusedAbsentCount, summary.absentCount)
        }
    }

    @Test
    fun `이전 기수가 마지막 소속이면 판정과 무관하게 COMPLETED 이고 그 기수 집계와 팀을 준다`() {
        val previous = cohorts.save(Cohort(value = "17")).id!!
        cohorts.save(Cohort(value = "18", isActive = true))
        val memberId = MemberId(1L)
        givenMember(memberId, cohort(1L, memberId, previous, "17"))
        attendances.memberDetails[memberId.value to previous.value] =
            detail(memberId, team = 4, summary = summary(present = 3, late = 1, excused = 1, offlineAbsent = 3))

        val response = service.memberMe(memberId, null)

        assertThat(response.cohort).isEqualTo("17")
        assertThat(response.isActiveCohort).isFalse()
        assertThat(response.attendanceStatus).isEqualTo("COMPLETED")
        assertThat(response.teamNumber).isEqualTo(TeamNumber(4))
        assertThat(listOf(response.presentCount, response.lateCount, response.excusedAbsentCount, response.absentCount))
            .containsExactly(3, 1, 1, 3)
    }

    /** 활성 기수가 하나도 없어도 기수 미배정 멤버는 조회된다. */
    @Test
    fun `기수 미배정이면 비활성, 집계 0, 수료 상태 null, 기본 팀이다`() {
        val memberId = MemberId(1L)
        givenMember(memberId)

        val response = service.memberMe(memberId, null)

        assertThat(response.cohort).isNull()
        assertThat(response.isActiveCohort).isFalse()
        assertThat(response.attendanceStatus).isNull()
        assertThat(response.teamNumber).isEqualTo(TeamNumber(0))
        assertThat(listOf(response.presentCount, response.lateCount, response.excusedAbsentCount, response.absentCount))
            .containsOnly(0)
    }

    @Test
    fun `기록이 없는 현재 기수 멤버는 집계 0 이고 NORMAL 이다`() {
        val current = cohorts.save(Cohort(value = "18", isActive = true)).id!!
        val memberId = MemberId(1L)
        givenMember(memberId, cohort(1L, memberId, current, "18"))
        attendances.memberDetails[memberId.value to current.value] =
            detail(memberId, team = 0, summary = summary(totalSessions = 10))

        val response = service.memberMe(memberId, null)

        assertThat(response.isActiveCohort).isTrue()
        assertThat(response.attendanceStatus).isEqualTo("NORMAL")
        assertThat(listOf(response.presentCount, response.lateCount, response.excusedAbsentCount, response.absentCount))
            .containsOnly(0)
    }

    @Test
    fun `마지막 소속 기수에 팀이 없으면 설정된 기본 팀이다`() {
        val current = cohorts.save(Cohort(value = "18", isActive = true)).id!!
        val memberId = MemberId(1L)
        givenMember(memberId, cohort(1L, memberId, current, "18"))
        attendances.memberDetails[memberId.value to current.value] =
            detail(memberId, team = 0, summary = summary(present = 1))

        val response = service(defaultTeamId = 99).memberMe(memberId, null)

        assertThat(response.teamNumber).isEqualTo(TeamNumber(99))
        assertThat(response.presentCount).isEqualTo(1)
    }

    /** 이전 기수 소속 행이 나중에 추가돼도 마지막 소속 기수(18기)의 기수, 팀, 집계를 같이 쓴다. */
    @Test
    fun `여러 기수와 팀 이력이 있으면 마지막 소속 기수의 팀과 집계만 쓴다`() {
        val previous = cohorts.save(Cohort(value = "17")).id!!
        val current = cohorts.save(Cohort(value = "18", isActive = true)).id!!
        val memberId = MemberId(1L)
        givenMember(memberId, cohort(1L, memberId, current, "18"), cohort(2L, memberId, previous, "17"))
        attendances.memberDetails[memberId.value to previous.value] =
            detail(memberId, team = 5, summary = summary(present = 1, offlineAbsent = 3))
        attendances.memberDetails[memberId.value to current.value] =
            detail(memberId, team = 1, summary = summary(present = 2, late = 1))

        val response = service.memberMe(memberId, null)

        assertThat(response.cohort).isEqualTo("18")
        assertThat(response.teamNumber).isEqualTo(TeamNumber(1))
        assertThat(response.isActiveCohort).isTrue()
        assertThat(response.attendanceStatus).isEqualTo("NORMAL")
        assertThat(listOf(response.presentCount, response.lateCount, response.absentCount)).containsExactly(2, 1, 0)
    }

    @Test
    fun `로그인 수단 이메일과 구형 토큰의 가입 이메일 대체는 그대로다`() {
        val memberId = MemberId(1L)
        givenMember(memberId)
        given(memberOAuthPersistencePort.findById(MemberOAuthId(7L)))
            .willReturn(MemberOAuth(MemberOAuthId(7L), "kakao-7", OAuthProvider.KAKAO, memberId, "kakao@kakao.com"))

        val kakao = service.memberMe(memberId, LoginIdentity(LoginMethod.KAKAO, 7L))
        val legacy = service.memberMe(memberId, null)

        assertThat(kakao.email).isEqualTo("kakao@kakao.com")
        assertThat(kakao.loginMethod).isEqualTo("KAKAO")
        assertThat(legacy.email).isEqualTo("signup@gmail.com")
        assertThat(legacy.loginMethod).isNull()
    }

    @Test
    fun `응답 JSON 은 기존 필드를 유지하고 새 필드를 평평하게 주며 null 수료 상태도 키로 준다`() {
        val memberId = MemberId(1L)
        givenMember(memberId)

        val json = jacksonObjectMapper().convertValue<Map<String, Any?>>(service.memberMe(memberId, null))

        assertThat(json).containsOnlyKeys(
            "email", "name", "part", "cohort", "teamNumber", "isAdmin", "status", "loginMethod",
            "isActiveCohort", "presentCount", "lateCount", "excusedAbsentCount", "absentCount", "attendanceStatus",
        )
        assertThat(json["teamNumber"]).isEqualTo(0)
        assertThat(json["isActiveCohort"]).isEqualTo(false)
        assertThat(json["presentCount"]).isEqualTo(0)
        assertThat(json).containsEntry("attendanceStatus", null)
    }

    private fun givenMember(
        memberId: MemberId,
        vararg memberCohorts: MemberCohort,
    ) {
        given(memberPersistencePort.findById(memberId)).willReturn(
            Member(
                id = memberId,
                name = "디프만",
                signupEmail = "signup@gmail.com",
                status = MemberStatus.ACTIVE,
                memberCohorts = memberCohorts.toList(),
            ),
        )
    }

    private fun cohort(
        id: Long,
        memberId: MemberId,
        cohortId: CohortId,
        value: String,
    ) = MemberCohort(MemberCohortId(id), memberId, cohortId, value)

    private fun detail(
        memberId: MemberId,
        team: Int,
        summary: AttendanceSummaryQueryModel,
    ) = MemberDetailAttendanceQueryModel(memberId.value, "디프만", TeamNumber(team), false, "SERVER", summary)

    private fun summary(
        totalSessions: Int = 16,
        present: Int = 0,
        late: Int = 0,
        excused: Int = 0,
        onlineAbsent: Int = 0,
        offlineAbsent: Int = 0,
    ) = AttendanceSummaryQueryModel(totalSessions, present, late, excused, onlineAbsent, offlineAbsent)
}

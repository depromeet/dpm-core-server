package core.application.member.application.service

import core.application.attendance.application.service.AttendanceGraduationEvaluator
import core.application.cohort.application.service.CohortQueryService
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.application.member.presentation.request.MemberManagementRequest
import core.application.member.presentation.request.MemberManagementRequest.ApprovalStatus
import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import core.domain.attendance.port.outbound.query.MemberAttendanceQueryModel
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberCohortPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.port.outbound.query.MemberManagementQueryModel
import core.domain.member.vo.MemberRoleAssignment
import core.domain.team.vo.TeamNumber
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

class MemberManagementQueryServiceTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val roles = mock(MemberRolePersistencePort::class.java)
    private val attendance = mock(AttendancePersistencePort::class.java)
    private val cohorts = mock(CohortQueryUseCase::class.java)
    private val resolver = CurrentCohortRoleResolver(mock(CohortQueryService::class.java), mock(MemberCohortPersistencePort::class.java), roles)
    private val service = MemberManagementQueryService(members, roles, attendance, cohorts, resolver, AttendanceGraduationEvaluator())
    private val updatedAt = Instant.parse("2026-10-06T02:00:00Z")
    private val source =
        listOf(
            member(1, "가", MemberPart.WEB),
            member(2, "나", MemberPart.SERVER),
            member(3, "다", MemberPart.WEB),
            member(4, "라", MemberPart.SERVER),
            member(5, "마", null, team = 0, status = MemberStatus.INACTIVE),
            member(6, "바", null, team = 0, status = MemberStatus.PENDING, cohortId = null),
            member(7, "사", MemberPart.WEB, status = MemberStatus.PENDING),
        )

    @BeforeEach
    fun setup() {
        `when`(cohorts.getLatestCohortId()).thenReturn(CohortId(19))
        `when`(members.findManagementMembers(19)).thenReturn(source)
        `when`(roles.findActiveRoleAssignmentsByMemberIds(source.map { it.memberId })).thenReturn(
            mapOf(
                1L to listOf(role("DEEPER"), role("ORGANIZER", 18)),
                2L to listOf(role("DEEPER")),
                3L to listOf(role("MASTER", null), role("CORE")),
                4L to listOf(role("ORGANIZER")),
                5L to listOf(role("MASTER", null)),
                // 소속 없는 회원은 역할만 있어도 현재 기수 인원에 포함하지 않는다.
                6L to listOf(role("DEEPER")),
                7L to listOf(role("GUEST", null)),
            ),
        )
        `when`(attendance.findMemberAttendances(19, emptyList())).thenReturn(
            listOf(attendanceRow(1, 3), attendanceRow(2, 5), attendanceRow(3), attendanceRow(4), attendanceRow(5), attendanceRow(7)),
        )
    }

    @Test
    fun `상단 현황은 검색과 운영진 제외에 영향받지 않고 각 미입력 회원은 한번만 센다`() {
        val response = service.getOverview(MemberManagementRequest(search = "가", part = "WEB"))
        assertThat(response.totalElements).isEqualTo(1)
        assertThat(response.members.map { it.memberId }).containsExactly(1L)
        assertThat(response.summary.totalMemberCount).isEqualTo(4)
        assertThat(response.summary.deeperCount).isEqualTo(2)
        assertThat(response.summary.coreCount).isEqualTo(1)
        assertThat(response.summary.organizerCount).isEqualTo(1)
        assertThat(response.summary.pendingCount).isEqualTo(2)
        assertThat(response.summary.missingInformationCount).isEqualTo(1)
        assertThat(response.summary.graduationRiskCount).isEqualTo(2)
        assertThat(response.lastUpdatedAt).isEqualTo(updatedAt)
        assertThat(response.members.single().duplicateSuspected).isNull()
    }

    @Test
    fun `위험 판정과 필터 후 페이지를 나누고 위험과 불가를 구분한다`() {
        val request = MemberManagementRequest(graduationStatuses = listOf(AttendanceGraduationStatus.AT_RISK, AttendanceGraduationStatus.IMPOSSIBLE), page = 2, size = 1)
        val response = service.getOverview(request)
        assertThat(response.totalElements).isEqualTo(2)
        assertThat(response.members.single().memberId).isEqualTo(2)
        assertThat(response.members.single().graduationStatus).isEqualTo(AttendanceGraduationStatus.IMPOSSIBLE)
        assertThat(service.getOverview(request.copy(page = 1)).members.single().graduationStatus).isEqualTo(AttendanceGraduationStatus.AT_RISK)
        assertThat(service.getOverview(request.copy(page = Int.MAX_VALUE)).members).isEmpty()
    }

    @Test
    fun `수료 필터에 빈 값이 섞여도 미평가 회원은 결과에 포함하지 않는다`() {
        // Spring MVC가 NORMAL, 을 바인딩한 결과와 동일한 입력이다.
        @Suppress("UNCHECKED_CAST")
        val statuses = listOf(AttendanceGraduationStatus.NORMAL, null) as List<AttendanceGraduationStatus>
        val response = service.getOverview(MemberManagementRequest(graduationStatuses = statuses))
        assertThat(response.totalElements).isEqualTo(1)
        assertThat(response.members.map { it.memberId }).containsExactly(5L)
        assertThat(response.members.single().graduationStatus).isEqualTo(AttendanceGraduationStatus.NORMAL)
        val pending = service.getOverview(MemberManagementRequest(approvalStatus = ApprovalStatus.PENDING, graduationStatuses = statuses))
        assertThat(pending.totalElements).isZero()
        assertThat(pending.members).isEmpty()
    }

    @Test
    fun `무소속 대기자는 미승인 목록에만 나오고 수료는 미평가다`() {
        val response = service.getOverview(MemberManagementRequest(approvalStatus = ApprovalStatus.PENDING))
        assertThat(response.members.map { it.memberId }).containsExactly(6L, 7L)
        assertThat(response.members.first().cohortId).isNull()
        assertThat(response.members.first().memberType).isEqualTo("UNASSIGNED")
        assertThat(response.members.map { it.graduationStatus }).containsOnlyNulls()
        assertThat(response.members.map { it.missingInformation }).containsOnly(false)
    }

    @Test
    fun `미배정과 활동 정지 필터를 조합하고 기본 조회는 운영진 코어만 제외한다`() {
        assertThat(service.getOverview(MemberManagementRequest()).members.map { it.memberId }).containsExactly(1L, 2L, 5L)
        val result = service.getOverview(MemberManagementRequest(missingInformationOnly = true, part = "UNASSIGNED", teamNumber = 0, status = "INACTIVE"))
        assertThat(result.members.single().memberId).isEqualTo(5)
        assertThat(result.members.single().memberType).isEqualTo("UNASSIGNED")
        assertThat(result.members.single().graduationStatus).isEqualTo(AttendanceGraduationStatus.NORMAL)
        assertThat(service.getOverview(MemberManagementRequest(excludeStaff = false)).members).hasSize(5)
    }

    @Test
    fun `가입 이메일을 검색하며 공백과 대소문자를 정규화하고 없는 결과는 빈 페이지다`() {
        assertThat(service.getOverview(MemberManagementRequest(search = "  USER1@EXAMPLE.COM ")).members.single().memberId).isEqualTo(1)
        val empty = service.getOverview(MemberManagementRequest(search = "없는이름"))
        assertThat(empty.totalElements).isZero()
        assertThat(empty.members).isEmpty()
        assertThat(empty.summary.totalMemberCount).isEqualTo(4)
    }

    @Test
    fun `회원별 조회 없이 회원수에 무관하게 각 port를 한번만 부른다`() {
        service.getOverview(MemberManagementRequest())
        verify(cohorts).getLatestCohortId()
        verify(members).findManagementMembers(19)
        verify(roles).findActiveRoleAssignmentsByMemberIds(source.map { it.memberId })
        verify(attendance).findMemberAttendances(19, emptyList())
        verifyNoMoreInteractions(cohorts, members, roles, attendance)
    }

    private fun member(
        id: Long,
        name: String,
        part: MemberPart?,
        team: Int = 1,
        status: MemberStatus = MemberStatus.ACTIVE,
        cohortId: Long? = 19,
    ) = MemberManagementQueryModel(id, name, "user$id@example.com", part, status, cohortId, team, updatedAt)

    private fun role(
        name: String,
        cohortId: Long? = 19,
    ) = MemberRoleAssignment(name, cohortId?.let(::CohortId))

    private fun attendanceRow(
        id: Long,
        absent: Int = 0,
    ): MemberAttendanceQueryModel {
        val member = source.single { it.memberId == id }
        return MemberAttendanceQueryModel(id, member.name, TeamNumber(member.teamNumber), false, member.part?.name, AttendanceSummaryQueryModel(20, 0, 0, 0, absent, 0))
    }
}

package core.application.member.application.service

import core.application.attendance.application.service.AttendanceGraduationEvaluator
import core.application.cohort.application.service.CohortQueryService
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.application.member.presentation.request.MemberManagementRequest
import core.application.member.presentation.request.MemberManagementRequest.ActivityStatus
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
        val response = service.getOverview(MemberManagementRequest(search = "가", parts = listOf("WEB")))
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
        assertThat(response.members.single().duplicateSuspected).isFalse()
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
        val result = service.getOverview(MemberManagementRequest(missingInformationOnly = true, parts = listOf("UNASSIGNED"), teamNumbers = listOf(0), activityStatuses = listOf(ActivityStatus.INACTIVE)))
        assertThat(result.members.single().memberId).isEqualTo(5)
        assertThat(result.members.single().memberType).isEqualTo("UNASSIGNED")
        assertThat(result.members.single().graduationStatus).isEqualTo(AttendanceGraduationStatus.NORMAL)
        assertThat(service.getOverview(MemberManagementRequest(excludeStaff = false)).members).hasSize(5)
    }

    @Test
    fun `파트와 팀은 각 그룹에서 OR로 그룹 사이는 AND로 적용한 뒤 페이지를 나눈다`() {
        val candidates =
            source.map {
                when (it.memberId) {
                    2L -> it.copy(part = MemberPart.DESIGN, teamNumber = 2)
                    3L -> it.copy(part = MemberPart.SERVER)
                    4L -> it.copy(part = MemberPart.DESIGN, teamNumber = 3)
                    else -> it
                }
            }
        `when`(members.findManagementMembers(19)).thenReturn(candidates)
        val request = MemberManagementRequest(parts = listOf("WEB", "DESIGN"), teamNumbers = listOf(1, 2), excludeStaff = false, page = 2, size = 1)
        val response = service.getOverview(request)
        assertThat(response.totalElements).isEqualTo(2)
        assertThat(response.members.single().memberId).isEqualTo(2L)
        assertThat(response.summary.totalMemberCount).isEqualTo(4)
        assertThat(response.summary.graduationRiskCount).isEqualTo(2)
        assertThat(service.getOverview(request.copy(page = 1)).members.single().memberId).isEqualTo(1L)
        assertThat(service.getOverview(request.copy(page = Int.MAX_VALUE)).members).isEmpty()
        val unassigned = service.getOverview(MemberManagementRequest(parts = listOf("WEB", "UNASSIGNED"), teamNumbers = listOf(0, 1)))
        assertThat(unassigned.members.map { it.memberId }).containsExactly(1L, 5L)
    }

    @Test
    fun `활동 정상 위험 정지를 구분하고 위험 카드는 정지한 위험 회원도 포함한다`() {
        `when`(attendance.findMemberAttendances(19, emptyList())).thenReturn(
            listOf(attendanceRow(1, 3), attendanceRow(2, 5), attendanceRow(3), attendanceRow(4), attendanceRow(5, 5), attendanceRow(7, 5)),
        )
        val request = MemberManagementRequest(excludeStaff = false)
        val normal = service.getOverview(request.copy(activityStatuses = listOf(ActivityStatus.NORMAL)))
        assertThat(normal.members.map { it.memberId }).containsExactly(3L, 4L)
        val risk = service.getOverview(request.copy(activityStatuses = listOf(ActivityStatus.AT_RISK)))
        assertThat(risk.members.map { it.memberId }).containsExactly(1L, 2L)
        val inactive = service.getOverview(request.copy(activityStatuses = listOf(ActivityStatus.INACTIVE)))
        assertThat(inactive.members.single().memberId).isEqualTo(5L)
        assertThat(inactive.members.single().graduationStatus).isEqualTo(AttendanceGraduationStatus.IMPOSSIBLE)
        val combined = service.getOverview(request.copy(activityStatuses = listOf(ActivityStatus.NORMAL, ActivityStatus.INACTIVE)))
        assertThat(combined.members.map { it.memberId }).containsExactly(3L, 4L, 5L)
        val card = service.getOverview(request.copy(graduationStatuses = listOf(AttendanceGraduationStatus.AT_RISK, AttendanceGraduationStatus.IMPOSSIBLE)))
        assertThat(card.members.map { it.memberId }).containsExactly(1L, 2L, 5L)
        assertThat(card.summary.graduationRiskCount).isEqualTo(card.totalElements)
        val intersection = service.getOverview(request.copy(activityStatuses = listOf(ActivityStatus.AT_RISK), graduationStatuses = listOf(AttendanceGraduationStatus.IMPOSSIBLE)))
        assertThat(intersection.members.single().memberId).isEqualTo(2L)
    }

    @Test
    fun `선택하지 않은 빈 필터는 전체를 유지하고 대기자는 활동 상태 선택에 포함하지 않는다`() {
        assertThat(service.getOverview(MemberManagementRequest(parts = emptyList(), teamNumbers = emptyList(), activityStatuses = emptyList())))
            .isEqualTo(service.getOverview(MemberManagementRequest()))
        val pending = service.getOverview(MemberManagementRequest(approvalStatus = ApprovalStatus.PENDING, activityStatuses = ActivityStatus.entries))
        assertThat(pending.members).isEmpty()
        assertThat(pending.summary.pendingCount).isEqualTo(2)
    }

    @Test
    fun `닉네임과 표시 이메일을 검색하며 이메일이 없는 회원도 조회한다`() {
        `when`(members.findManagementMembers(19)).thenReturn(source.map { if (it.memberId == 5L) it.copy(email = null) else it })
        assertThat(service.getOverview(MemberManagementRequest(search = "  USER1@EXAMPLE.COM ")).members.single().memberId).isEqualTo(1)
        assertThat(service.getOverview(MemberManagementRequest(search = "마")).members.single().email).isNull()
        val empty = service.getOverview(MemberManagementRequest(search = "없는이름"))
        assertThat(empty.totalElements).isZero()
        assertThat(empty.members).isEmpty()
        assertThat(empty.summary.totalMemberCount).isEqualTo(4)
    }

    @Test
    fun `앞뒤 공백을 제외한 닉네임과 실제 파트가 모두 같아야 중복 의심이다`() {
        val candidates =
            listOf(
                member(1, "닉네임", MemberPart.WEB),
                member(2, "  닉네임  ", MemberPart.WEB),
                member(3, "닉네임", MemberPart.SERVER),
                member(4, "다른닉", MemberPart.WEB).copy(email = "user1@example.com"),
                member(5, "Nick", MemberPart.SERVER),
                member(6, "nick", MemberPart.SERVER),
                member(7, "미배정", null),
                member(8, "미배정", null),
                member(9, " ", MemberPart.WEB),
                member(10, "", MemberPart.WEB),
            )
        `when`(members.findManagementMembers(19)).thenReturn(candidates)
        val result = service.getOverview(MemberManagementRequest(excludeStaff = false)).members.associateBy { it.memberId }
        assertThat(result.filterValues { it.duplicateSuspected }.keys).containsExactlyInAnyOrder(1L, 2L)
        assertThat(result.getValue(2).name).isEqualTo("  닉네임  ")
        assertThat(result.filterKeys { it !in setOf(1L, 2L) }.values.map { it.duplicateSuspected }).containsOnly(false)
    }

    @Test
    fun `비교 상대가 다른 탭 검색 팀 필터 페이지나 운영진 제외로 숨겨져도 중복 배지를 유지한다`() {
        val candidates =
            listOf(
                member(1, "닉네임", MemberPart.WEB, team = 1),
                member(2, "닉네임", MemberPart.WEB, team = 2),
                member(3, "코어별명", MemberPart.SERVER),
                member(6, "닉네임", MemberPart.WEB, status = MemberStatus.PENDING, cohortId = null),
                member(7, "코어별명", MemberPart.SERVER, status = MemberStatus.PENDING),
            )
        `when`(members.findManagementMembers(19)).thenReturn(candidates)
        `when`(roles.findActiveRoleAssignmentsByMemberIds(candidates.map { it.memberId })).thenReturn(mapOf(3L to listOf(role("CORE"))))
        val filtered = service.getOverview(MemberManagementRequest(search = "user1@example.com", parts = listOf("WEB"), teamNumbers = listOf(1)))
        assertThat(filtered.members.single().memberId).isEqualTo(1L)
        assertThat(filtered.members.single().duplicateSuspected).isTrue()
        val page = service.getOverview(MemberManagementRequest(page = 2, size = 1))
        assertThat(page.members.single().memberId).isEqualTo(2L)
        assertThat(page.members.single().duplicateSuspected).isTrue()
        val pending = service.getOverview(MemberManagementRequest(approvalStatus = ApprovalStatus.PENDING))
        assertThat(pending.members.map { it.memberId }).containsExactly(6L, 7L)
        assertThat(pending.members.map { it.duplicateSuspected }).containsOnly(true)
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

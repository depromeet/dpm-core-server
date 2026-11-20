package core.application.member.application.service

import core.application.member.application.exception.InvalidMemberApprovalException
import core.application.member.application.exception.InvalidMemberManagementTeamException
import core.application.member.application.exception.MemberApprovalTargetNotAllowedException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberNotFoundException
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.port.outbound.query.CohortTeamQueryModel
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.MemberCohort
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberCohortPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.port.outbound.MemberTeamPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.vo.MemberId
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

class MemberApprovalServiceTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val memberCohorts = mock(MemberCohortPersistencePort::class.java)
    private val teams = mock(MemberTeamPersistencePort::class.java)
    private val roles = mock(MemberRolePersistencePort::class.java)
    private val cohorts = mock(CohortQueryUseCase::class.java)
    private val roleQueries = mock(RoleQueryUseCase::class.java)
    private val initializer = mock(MemberActivationInitializer::class.java)
    private val activities =
        mock(MemberActivityService::class.java).also { activity ->
            `when`(activity.lockCurrentCohort()).thenReturn(CohortId(19))
        }
    private val cohortTeams = mock(CohortPersistencePort::class.java)
    private val service =
        MemberApprovalService(members, memberCohorts, teams, roles, cohorts, roleQueries, initializer, activities, cohortTeams)

    @BeforeEach
    fun setup() {
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(19))
        `when`(roleQueries.findIdByName("DEEPER")).thenReturn(1)
        `when`(roleQueries.findIdByName("CORE")).thenReturn(3)
        `when`(cohortTeams.findTeamsByCohortId(CohortId(19))).thenReturn(listOf(CohortTeamQueryModel(id = 70, number = 1)))
        `when`(roles.replaceCurrentCohortRoles(listOf(1, 2), 19, 3)).thenReturn(setOf(1, 2))
        `when`(teams.replaceCurrentCohortTeams(listOf(1, 2), 19, 70)).thenReturn(setOf(1))
    }

    @Test
    fun `정보 미제출 회원이 포함된 승인은 전체를 변경 전에 거절한다`() {
        `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(
            listOf(
                MemberApprovalTarget(1, MemberStatus.PENDING, emptySet(), profileCompletedAt = java.time.Instant.EPOCH),
                MemberApprovalTarget(2, MemberStatus.PENDING, emptySet()),
            ),
        )
        assertThatThrownBy { service.approve(listOf(1, 2)) }
            .isInstanceOf(core.application.common.exception.BusinessException::class.java)
            .satisfies(
                java.util.function.Consumer<Throwable> { exception ->
                    org.assertj.core.api.Assertions.assertThat((exception as core.application.common.exception.BusinessException).getCode())
                        .isEqualTo(core.application.member.application.exception.MemberExceptionCode.MEMBER_PROFILE_COMPLETION_REQUIRED)
                },
            )
        verifyNoInteractions(memberCohorts, teams, roles, roleQueries, initializer)
    }

    @Test
    fun `빈 중복 null 0 음수 ID는 조회 전에 거절한다`() {
        listOf(emptyList(), listOf(1L, 1L), listOf(null), listOf(0L), listOf(-1L)).forEach { ids ->
            assertThatThrownBy { service.approve(ids) }.isInstanceOf(InvalidMemberApprovalException::class.java)
        }
        verifyNoInteractions(members, memberCohorts, teams, roles, cohorts, roleQueries, initializer, activities)
    }

    @Test
    fun `과거 기수 탈퇴 대상을 포함하면 어떤 멤버도 변경하지 않는다`() {
        listOf(
            listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet(), profileCompletedAt = java.time.Instant.EPOCH), MemberApprovalTarget(2, MemberStatus.PENDING, setOf(18))),
            listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet(), profileCompletedAt = java.time.Instant.EPOCH), MemberApprovalTarget(2, MemberStatus.WITHDRAWN, setOf(19))),
        ).forEach { targets ->
            `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(targets)
            assertThatThrownBy { service.approve(listOf(2, 1)) }.isInstanceOf(MemberApprovalTargetNotAllowedException::class.java)
        }
        verifyNoInteractions(memberCohorts, teams, roles, roleQueries, initializer)
    }

    @Test
    fun `누락 삭제 회원은 기존 예외를 재사용한다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(emptyList())
        assertThatThrownBy { service.approve(listOf(1)) }.isInstanceOf(MemberNotFoundException::class.java)
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(
            listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet(), isDeleted = true)),
        )
        assertThatThrownBy { service.approve(listOf(1)) }.isInstanceOf(MemberDeletedException::class.java)
        verifyNoInteractions(memberCohorts, teams, roles, roleQueries, initializer)
    }

    @Test
    fun `대상 잠금을 정렬하고 새 대기자만 변경하며 이미 승인된 멤버는 유지한다`() {
        `when`(members.lockApprovalTargets(listOf(1, 2, 3))).thenReturn(
            listOf(
                MemberApprovalTarget(1, MemberStatus.PENDING, emptySet(), profileCompletedAt = java.time.Instant.EPOCH),
                MemberApprovalTarget(2, MemberStatus.ACTIVE, setOf(19)),
                MemberApprovalTarget(3, MemberStatus.ACTIVE, setOf(19)),
            ),
        )
        service.approve(listOf(3, 1, 2))
        verify(members).lockApprovalTargets(listOf(1, 2, 3))
        verify(roles).replaceCurrentCohortRoles(listOf(1), 19, 1)
        verify(teams).replaceCurrentCohortTeams(listOf(1), 19, null)
        verify(members).updateManagementFields(listOf(1), false, null, MemberStatus.ACTIVE, emptySet())
        verify(members).resetApprovalWelcome(listOf(1))
        verify(initializer).initialize(MemberId(1), CohortId(19))
    }

    @Test
    fun `승인된 현재 기수 멤버만 재요청하면 초기화도 하지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, setOf(19))))
        service.approve(listOf(1))
        verify(members).lockApprovalTargets(listOf(1))
        verifyNoMoreInteractions(members)
        verifyNoInteractions(memberCohorts, teams, roles, roleQueries, initializer)
    }

    @Test
    fun `기존 회원의 새 기수 승인은 승인 화면 확인을 초기화하지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.INACTIVE, setOf(18))))
        service.approve(listOf(1))
        verify(members).resetApprovalWelcome(emptyList())
        verify(initializer).initialize(MemberId(1), CohortId(19))
    }

    @Test
    fun `지난 기수 회원과 가입 대기자를 지정한 타입과 현재 기수 팀으로 함께 승인하고 지난 기수 기록은 건드리지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(
            listOf(
                MemberApprovalTarget(1, MemberStatus.INACTIVE, setOf(18)),
                MemberApprovalTarget(2, MemberStatus.PENDING, emptySet(), profileCompletedAt = java.time.Instant.EPOCH),
            ),
        )
        service.approve(listOf(2, 1), memberType = "CORE", teamId = 70)
        verify(roles).replaceCurrentCohortRoles(listOf(1, 2), 19, 3)
        verify(teams).replaceCurrentCohortTeams(listOf(1, 2), 19, 70)
        verify(memberCohorts).save(MemberCohort.of(MemberId(1), CohortId(19)))
        verify(memberCohorts).save(MemberCohort.of(MemberId(2), CohortId(19)))
        verifyNoMoreInteractions(memberCohorts, teams, roles)
        verify(members).updateManagementFields(listOf(1, 2), false, null, MemberStatus.ACTIVE, setOf(1, 2))
        verify(members).resetApprovalWelcome(listOf(2))
        verify(initializer).initialize(MemberId(1), CohortId(19))
        verify(initializer).initialize(MemberId(2), CohortId(19))
    }

    @Test
    fun `팀 ID 0은 미배정이고 타입 생략은 디퍼다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.INACTIVE, setOf(18))))
        service.approve(listOf(1), teamId = 0)
        verify(roles).replaceCurrentCohortRoles(listOf(1), 19, 1)
        verify(teams).replaceCurrentCohortTeams(listOf(1), 19, null)
        verifyNoInteractions(cohortTeams)
    }

    @Test
    fun `현재 기수 팀이 아닌 팀 ID는 어떤 멤버도 변경하기 전에 거절한다`() {
        `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(
            listOf(
                MemberApprovalTarget(1, MemberStatus.INACTIVE, setOf(18)),
                MemberApprovalTarget(2, MemberStatus.ACTIVE, setOf(19)),
            ),
        )
        assertThatThrownBy { service.approve(listOf(1, 2), teamId = 71) }
            .isInstanceOf(InvalidMemberManagementTeamException::class.java)
        verifyNoInteractions(memberCohorts, teams, roles, roleQueries, initializer)
        verify(members).lockApprovalTargets(listOf(1, 2))
        verifyNoMoreInteractions(members)
    }

    @Test
    fun `지원하지 않는 타입과 음수 팀 ID는 조회 전에 거절한다`() {
        listOf("MASTER" to null, "UNASSIGNED" to null, "deeper" to null, null to -1L).forEach { (type, team) ->
            assertThatThrownBy { service.approve(listOf(1), type, team) }.isInstanceOf(InvalidMemberApprovalException::class.java)
        }
        verifyNoInteractions(members, memberCohorts, teams, roles, cohorts, roleQueries, initializer, activities, cohortTeams)
    }

    @Test
    fun `이미 현재 기수에 승인된 멤버는 타입과 팀을 보내도 바꾸지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, setOf(18, 19))))
        service.approve(listOf(1), memberType = "CORE", teamId = 70)
        verify(members).lockApprovalTargets(listOf(1))
        verifyNoMoreInteractions(members)
        verifyNoInteractions(memberCohorts, teams, roles, roleQueries, initializer)
    }
}

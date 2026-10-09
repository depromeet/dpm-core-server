package core.application.member.application.service

import core.application.member.application.exception.InvalidMemberApprovalException
import core.application.member.application.exception.MemberApprovalTargetNotAllowedException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberNotFoundException
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
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
    private val service = MemberApprovalService(members, memberCohorts, teams, roles, cohorts, roleQueries, initializer)

    @BeforeEach
    fun setup() {
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(19))
        `when`(roleQueries.findIdByName("DEEPER")).thenReturn(1)
    }

    @Test
    fun `빈 중복 null 0 음수 ID는 조회 전에 거절한다`() {
        listOf(emptyList(), listOf(1L, 1L), listOf(null), listOf(0L), listOf(-1L)).forEach { ids ->
            assertThatThrownBy { service.approve(ids) }.isInstanceOf(InvalidMemberApprovalException::class.java)
        }
        verifyNoInteractions(members, memberCohorts, teams, roles, cohorts, roleQueries, initializer)
    }

    @Test
    fun `과거 기수 탈퇴 대상을 포함하면 어떤 멤버도 변경하지 않는다`() {
        listOf(
            listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet()), MemberApprovalTarget(2, MemberStatus.PENDING, setOf(18))),
            listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet()), MemberApprovalTarget(2, MemberStatus.ACTIVE, setOf(18))),
            listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet()), MemberApprovalTarget(2, MemberStatus.WITHDRAWN, setOf(19))),
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
                MemberApprovalTarget(1, MemberStatus.PENDING, emptySet()),
                MemberApprovalTarget(2, MemberStatus.ACTIVE, setOf(19)),
                MemberApprovalTarget(3, MemberStatus.INACTIVE, setOf(19)),
            ),
        )
        service.approve(listOf(3, 1, 2))
        verify(members).lockApprovalTargets(listOf(1, 2, 3))
        verify(roles).replaceCurrentCohortRoles(listOf(1), 19, 1)
        verify(teams).replaceCurrentCohortTeams(listOf(1), 19, null)
        verify(members).updateManagementFields(listOf(1), false, null, MemberStatus.ACTIVE, emptySet())
        verify(initializer).initialize(MemberId(1), CohortId(19))
    }

    @Test
    fun `승인된 현재 기수 멤버만 재요청하면 초기화도 하지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.INACTIVE, setOf(19))))
        service.approve(listOf(1))
        verify(members).lockApprovalTargets(listOf(1))
        verifyNoMoreInteractions(members)
        verifyNoInteractions(memberCohorts, teams, roles, roleQueries, initializer)
    }
}

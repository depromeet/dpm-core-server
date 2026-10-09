package core.application.member.application.service

import core.application.member.application.exception.InvalidMemberManagementTeamException
import core.application.member.application.exception.InvalidMemberManagementUpdateException
import core.application.member.application.exception.MemberManagementTargetNotAllowedException
import core.application.member.presentation.request.MemberManagementBulkUpdateRequest
import core.application.member.presentation.request.MemberManagementUpdateRequest
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.port.outbound.query.CohortTeamQueryModel
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.port.outbound.MemberTeamPersistencePort
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

class MemberManagementCommandServiceTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val teams = mock(MemberTeamPersistencePort::class.java)
    private val roles = mock(MemberRolePersistencePort::class.java)
    private val cohorts = mock(CohortQueryUseCase::class.java)
    private val cohortPort = mock(CohortPersistencePort::class.java)
    private val roleQueries = mock(RoleQueryUseCase::class.java)
    private val service = MemberManagementCommandService(members, teams, roles, cohorts, cohortPort, roleQueries)

    @BeforeEach
    fun setup() {
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(19))
        `when`(members.lockApprovedManagementMemberIds(listOf(1L), 19)).thenReturn(listOf(1L))
        `when`(members.lockApprovedManagementMemberIds(listOf(1L, 2L), 19)).thenReturn(listOf(1L, 2L))
        `when`(cohortPort.findTeamsByCohortId(CohortId(19))).thenReturn(listOf(CohortTeamQueryModel(42, 3)))
        `when`(roleQueries.findIdByName("CORE")).thenReturn(3)
    }

    @Test
    fun `단건의 네 컬럼을 검증한 뒤 함께 변경한다`() {
        `when`(teams.replaceCurrentCohortTeams(listOf(1L), 19, 42)).thenReturn(setOf(1))
        `when`(roles.replaceCurrentCohortRoles(listOf(1L), 19, 3)).thenReturn(setOf(1))
        service.update(1, MemberManagementUpdateRequest("SERVER", 42, "CORE", "INACTIVE"))

        val order = inOrder(members, cohortPort, roleQueries, teams, roles)
        order.verify(members).lockApprovedManagementMemberIds(listOf(1L), 19)
        order.verify(cohortPort).findTeamsByCohortId(CohortId(19))
        order.verify(roleQueries).findIdByName("CORE")
        order.verify(teams).replaceCurrentCohortTeams(listOf(1L), 19, 42)
        order.verify(roles).replaceCurrentCohortRoles(listOf(1L), 19, 3)
        order.verify(members).updateManagementFields(listOf(1L), true, MemberPart.SERVER, MemberStatus.INACTIVE, setOf(1))
        order.verifyNoMoreInteractions()
    }

    @Test
    fun `일괄 요청의 ID를 정렬하고 선택한 컬럼만 변경한다`() {
        service.updateBulk(MemberManagementBulkUpdateRequest(listOf(2, 1), MemberManagementUpdateRequest(part = "SERVER")))
        verify(members).lockApprovedManagementMemberIds(listOf(1L, 2L), 19)
        verify(members).updateManagementFields(listOf(1L, 2L), true, MemberPart.SERVER, null, emptySet())
        verifyNoMoreInteractions(members)
        verifyNoInteractions(teams, roles, cohortPort, roleQueries)
    }

    @Test
    fun `미배정은 명시적으로 해제하고 생략한 값과 구분한다`() {
        service.update(1, MemberManagementUpdateRequest(part = "UNASSIGNED", teamId = 0, memberType = "UNASSIGNED"))
        verify(teams).replaceCurrentCohortTeams(listOf(1L), 19, null)
        verify(roles).replaceCurrentCohortRoles(listOf(1L), 19, null)
        verify(members).updateManagementFields(listOf(1L), true, null, null, emptySet())
        verifyNoInteractions(cohortPort, roleQueries)
    }

    @Test
    fun `빈 중복 null 음수 ID와 한 컬럼이 아닌 일괄 요청을 거절한다`() {
        val part = MemberManagementUpdateRequest(part = "SERVER")
        listOf(
            MemberManagementBulkUpdateRequest(emptyList(), part),
            MemberManagementBulkUpdateRequest(listOf(1, 1), part),
            MemberManagementBulkUpdateRequest(listOf(1, null), part),
            MemberManagementBulkUpdateRequest(listOf(0), part),
            MemberManagementBulkUpdateRequest(listOf(-1), part),
            MemberManagementBulkUpdateRequest(listOf(1), MemberManagementUpdateRequest()),
            MemberManagementBulkUpdateRequest(listOf(1), part.copy(status = "INACTIVE")),
        ).forEach { request ->
            assertThatThrownBy { service.updateBulk(request) }.isInstanceOf(InvalidMemberManagementUpdateException::class.java)
        }
        verifyNoInteractions(members, teams, roles, cohorts, cohortPort, roleQueries)
    }

    @Test
    fun `단건의 빈 변경과 허용하지 않는 파트 타입 활동 상태를 거절한다`() {
        listOf(
            MemberManagementUpdateRequest(),
            MemberManagementUpdateRequest(part = "GUEST"),
            MemberManagementUpdateRequest(memberType = "MASTER"),
            MemberManagementUpdateRequest(status = "PENDING"),
            MemberManagementUpdateRequest(status = "AT_RISK"),
            MemberManagementUpdateRequest(teamId = -1),
        ).forEach { changes ->
            assertThatThrownBy { service.update(1, changes) }.isInstanceOf(InvalidMemberManagementUpdateException::class.java)
        }
        assertThatThrownBy { service.update(0, MemberManagementUpdateRequest(part = "SERVER")) }
            .isInstanceOf(InvalidMemberManagementUpdateException::class.java)
        verifyNoInteractions(members, teams, roles, cohorts, cohortPort, roleQueries)
    }

    @Test
    fun `대상 중 하나라도 수정 불가능하면 모든 쓰기를 막는다`() {
        `when`(members.lockApprovedManagementMemberIds(listOf(1L, 2L), 19)).thenReturn(listOf(1L))
        assertThatThrownBy {
            service.updateBulk(MemberManagementBulkUpdateRequest(listOf(1, 2), MemberManagementUpdateRequest(part = "SERVER")))
        }.isInstanceOf(MemberManagementTargetNotAllowedException::class.java)
        verify(members).lockApprovedManagementMemberIds(listOf(1L, 2L), 19)
        verifyNoMoreInteractions(members)
        verifyNoInteractions(teams, roles, cohortPort, roleQueries)
    }

    @Test
    fun `다른 기수 팀은 수정 전에 거절한다`() {
        assertThatThrownBy { service.update(1, MemberManagementUpdateRequest(part = "SERVER", teamId = 999)) }
            .isInstanceOf(InvalidMemberManagementTeamException::class.java)
        verify(members).lockApprovedManagementMemberIds(listOf(1L), 19)
        verifyNoMoreInteractions(members)
        verifyNoInteractions(teams, roles, roleQueries)
    }

    @Test
    fun `팀이나 타입만 변경해도 갱신할 멤버 ID를 전달한다`() {
        `when`(teams.replaceCurrentCohortTeams(listOf(1L, 2L), 19, 42)).thenReturn(setOf(2))
        service.updateBulk(MemberManagementBulkUpdateRequest(listOf(1, 2), MemberManagementUpdateRequest(teamId = 42)))
        verify(members).updateManagementFields(listOf(1L, 2L), false, null, null, setOf(2))
        verifyNoInteractions(roles, roleQueries)
    }
}

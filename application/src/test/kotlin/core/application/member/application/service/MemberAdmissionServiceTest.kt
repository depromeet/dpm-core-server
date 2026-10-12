package core.application.member.application.service

import core.application.cohort.application.exception.CohortNotFoundException
import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.service.cohort.MemberCohortService
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.MemberAdmissionEventType
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberAdmissionEventPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.vo.MemberId
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

class MemberAdmissionServiceTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val events = mock(MemberAdmissionEventPersistencePort::class.java)
    private val cohorts = mock(CohortQueryUseCase::class.java)
    private val memberCohorts = mock(MemberCohortService::class.java)
    private val service = MemberAdmissionService(members, events, cohorts, memberCohorts)

    @Test
    fun `잘못된 ID는 잠금 조회 전에 거절한다`() {
        listOf(0L, -1L).forEach {
            assertThatThrownBy { service.reject(it) }.isInstanceOf(BusinessException::class.java)
            assertThatThrownBy { service.reapply(it) }.isInstanceOf(BusinessException::class.java)
        }
        verifyNoInteractions(members, events, cohorts)
    }

    @Test
    fun `누락 삭제 회원은 이력과 상태를 변경하지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(emptyList())
        assertThatThrownBy { service.reject(1) }.isInstanceOf(MemberNotFoundException::class.java)
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.REJECTED, emptySet(), true)))
        assertThatThrownBy { service.reapply(1) }.isInstanceOf(MemberDeletedException::class.java)
        verifyNoInteractions(events)
    }

    @Test
    fun `현재 기수와 무소속 대기자만 반려한다`() {
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(19))
        listOf(emptySet(), setOf(19L), setOf(18L, 19L)).forEachIndexed { index, cohortIds ->
            val id = index + 1L
            `when`(members.lockApprovalTargets(listOf(id))).thenReturn(listOf(MemberApprovalTarget(id, MemberStatus.PENDING, cohortIds)))
            service.reject(id)
            verify(events).record(id, MemberAdmissionEventType.REJECTED)
            verify(members).updateManagementFields(listOf(id), false, null, MemberStatus.REJECTED, emptySet())
        }
    }

    @Test
    fun `과거 기수 및 반복 반려 승인 탈퇴는 거절한다`() {
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(19))
        listOf(
            MemberApprovalTarget(1, MemberStatus.PENDING, setOf(18)),
            MemberApprovalTarget(1, MemberStatus.REJECTED, setOf(19)),
            MemberApprovalTarget(1, MemberStatus.ACTIVE, setOf(19)),
            MemberApprovalTarget(1, MemberStatus.INACTIVE, setOf(19)),
            MemberApprovalTarget(1, MemberStatus.WITHDRAWN, setOf(19)),
        ).forEach {
            `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(it))
            assertThatThrownBy { service.reject(1) }.isInstanceOf(BusinessException::class.java)
        }
        verifyNoInteractions(events)
    }

    @Test
    fun `반려 회원 재신청만 기록하고 이미 대기면 변경하지 않는다`() {
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(20))
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.REJECTED, emptySet())))
        service.reapply(1)
        verify(events).record(1, MemberAdmissionEventType.REAPPLIED)
        verify(members).updateManagementFields(listOf(1), false, null, MemberStatus.PENDING, emptySet())
        `when`(members.lockApprovalTargets(listOf(2))).thenReturn(listOf(MemberApprovalTarget(2, MemberStatus.PENDING, emptySet())))
        service.reapply(2)
        verifyNoMoreInteractions(events)
        verify(cohorts).getActiveCohortId()
        verify(memberCohorts).addMemberToCohort(MemberId(1), CohortId(20))
        verifyNoMoreInteractions(cohorts, memberCohorts)
    }

    @Test
    fun `기수가 없으면 재신청 이력과 소속과 상태를 변경하지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.REJECTED, setOf(19))))
        `when`(cohorts.getActiveCohortId()).thenThrow(CohortNotFoundException())
        assertThatThrownBy { service.reapply(1) }.isInstanceOf(CohortNotFoundException::class.java)
        verify(members).lockApprovalTargets(listOf(1))
        verifyNoMoreInteractions(members)
        verifyNoInteractions(events, memberCohorts)
    }

    @Test
    fun `이미 대기인 재요청은 현재 기수를 조회하지 않고 과거 소속을 유지한다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.PENDING, setOf(19))))
        service.reapply(1)
        verify(members).lockApprovalTargets(listOf(1))
        verifyNoMoreInteractions(members)
        verifyNoInteractions(events, cohorts, memberCohorts)
    }

    @Test
    fun `승인 및 탈퇴 회원은 재신청하지 못한다`() {
        listOf(MemberStatus.ACTIVE, MemberStatus.INACTIVE, MemberStatus.WITHDRAWN).forEach {
            `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, it, setOf(19))))
            assertThatThrownBy { service.reapply(1) }.isInstanceOf(BusinessException::class.java)
        }
        verifyNoInteractions(events)
    }
}

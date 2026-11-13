package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberSharedRecordsException
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.vo.MemberId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher

class MemberDeletionServiceTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val events = mock(ApplicationEventPublisher::class.java)
    private val activities = mock(MemberActivityService::class.java)
    private val cohorts = mock(CohortQueryUseCase::class.java)
    private val service = MemberDeletionService(members, activities, cohorts, events)

    @BeforeEach
    fun setUp() {
        `when`(cohorts.findActiveCohortId()).thenReturn(CohortId(19))
    }

    @Test
    fun `차단된 승인 회원 ID 전체를 반환하고 어떤 회원도 변경하지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1, 2, 3, 4))).thenReturn(
            listOf(
                MemberApprovalTarget(1, MemberStatus.PENDING, emptySet()),
                MemberApprovalTarget(2, MemberStatus.ACTIVE, ACTIVE_COHORT),
                MemberApprovalTarget(3, MemberStatus.INACTIVE, ACTIVE_COHORT),
                MemberApprovalTarget(4, MemberStatus.ACTIVE, ACTIVE_COHORT),
            ),
        )
        `when`(members.findSharedRecordOwnerIds(listOf(2, 3, 4))).thenReturn(listOf(2, 4))
        val exception = assertThrows<MemberSharedRecordsException> { service.delete(listOf(4, 1, 3, 2), 99) }
        assertThat(exception.blockedMemberIds).containsExactly(2, 4)
        verify(members).lockApprovalTargets(listOf(1, 2, 3, 4))
        verify(members).findSharedRecordOwnerIds(listOf(2, 3, 4))
        verifyNoMoreInteractions(members)
        verifyNoInteractions(events)
    }

    @Test
    fun `본인 계정이 포함되면 단건과 일괄 모두 조회 전에 거절한다`() {
        listOf(listOf(1L), listOf(2L, 1L)).forEach {
            assertThatThrownBy { service.delete(it, 1) }
                .isInstanceOf(BusinessException::class.java)
                .hasMessage(MemberExceptionCode.MEMBER_SELF_DELETION_NOT_ALLOWED.message)
        }
        verifyNoInteractions(members)
    }

    @Test
    fun `빈 중복 null 0 음수 목록은 조회 전에 거절한다`() {
        listOf(emptyList(), listOf(1L, 1L), listOf(null), listOf(0L), listOf(-1L)).forEach {
            assertThatThrownBy { service.delete(it, 99) }.isInstanceOf(BusinessException::class.java)
        }
        verifyNoInteractions(members)
    }

    @Test
    fun `삭제할 수 없는 대상이 하나라도 있으면 변경하지 않는다`() {
        listOf(
            emptyList(),
            listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, ACTIVE_COHORT)),
            listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, ACTIVE_COHORT), MemberApprovalTarget(2, MemberStatus.ACTIVE, ACTIVE_COHORT, true)),
            listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, ACTIVE_COHORT), MemberApprovalTarget(2, MemberStatus.WITHDRAWN, emptySet())),
        ).forEach { targets ->
            `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(targets)
            assertThatThrownBy { service.delete(listOf(2, 1), 99) }.isInstanceOf(MemberNotFoundException::class.java)
        }
        verify(members, org.mockito.Mockito.times(4)).lockApprovalTargets(listOf(1, 2))
        verifyNoMoreInteractions(members)
    }

    @Test
    fun `미승인과 반려는 소프트 삭제하고 승인 회원은 영구 삭제한다`() {
        val statuses = listOf(MemberStatus.PENDING, MemberStatus.REJECTED, MemberStatus.ACTIVE, MemberStatus.INACTIVE)
        val ids = listOf(1L, 2L, 3L, 4L)
        `when`(members.lockApprovalTargets(ids)).thenReturn(statuses.mapIndexed { index, status -> MemberApprovalTarget(index.toLong() + 1, status, if (status == MemberStatus.PENDING || status == MemberStatus.REJECTED) emptySet() else ACTIVE_COHORT) })
        `when`(members.hardDeleteById(MemberId(3))).thenReturn(emptyList())
        `when`(members.hardDeleteById(MemberId(4))).thenReturn(emptyList())
        service.delete(ids.reversed(), 99)
        verify(members).softDeleteMembers(listOf(1, 2))
        verify(members).hardDeleteById(MemberId(3))
        verify(members).hardDeleteById(MemberId(4))
        verify(events).publishEvent(MemberPurgedEvent(3, emptyList()))
        verify(events).publishEvent(MemberPurgedEvent(4, emptyList()))
    }

    @Test
    fun `공유 기록 소유자가 포함되면 미승인 삭제도 시작하지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(
            listOf(
                MemberApprovalTarget(1, MemberStatus.PENDING, emptySet()),
                MemberApprovalTarget(2, MemberStatus.ACTIVE, ACTIVE_COHORT),
            ),
        )
        `when`(members.findSharedRecordOwnerIds(listOf(2))).thenReturn(listOf(2L))
        assertThatThrownBy { service.delete(listOf(1, 2), 99) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessage(MemberExceptionCode.MEMBER_SHARED_RECORDS_EXIST.message)
        verify(members).lockApprovalTargets(listOf(1, 2))
        verify(members).findSharedRecordOwnerIds(listOf(2))
        verifyNoMoreInteractions(members)
        verifyNoInteractions(events)
    }

    @Test
    fun `지난 기수에만 속한 승인 회원이 있으면 일괄 삭제 전체를 거절하고 아무것도 바꾸지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1, 2, 3))).thenReturn(
            listOf(
                MemberApprovalTarget(1, MemberStatus.PENDING, emptySet()),
                MemberApprovalTarget(2, MemberStatus.ACTIVE, ACTIVE_COHORT),
                MemberApprovalTarget(3, MemberStatus.INACTIVE, setOf(18L)),
            ),
        )
        assertThatThrownBy { service.delete(listOf(1, 2, 3), 99) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessage(MemberExceptionCode.MEMBER_PAST_COHORT_DELETION_NOT_ALLOWED.message)
        verify(members).lockApprovalTargets(listOf(1, 2, 3))
        verifyNoMoreInteractions(members)
        verifyNoInteractions(events)
    }

    @Test
    fun `지난 기수와 활성 기수에 모두 속한 승인 회원은 삭제할 수 있다`() {
        `when`(members.lockApprovalTargets(listOf(5))).thenReturn(listOf(MemberApprovalTarget(5, MemberStatus.ACTIVE, setOf(18L, 19L))))
        `when`(members.hardDeleteById(MemberId(5))).thenReturn(emptyList())
        service.delete(listOf(5), 99)
        verify(members).hardDeleteById(MemberId(5))
    }

    @Test
    fun `활성 기수가 없으면 승인 회원은 거절하고 가입 대기자는 삭제한다`() {
        `when`(cohorts.findActiveCohortId()).thenReturn(null)
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, setOf(18L))))
        assertThatThrownBy { service.delete(listOf(1), 99) }
            .hasMessage(MemberExceptionCode.MEMBER_PAST_COHORT_DELETION_NOT_ALLOWED.message)
        `when`(members.lockApprovalTargets(listOf(2))).thenReturn(listOf(MemberApprovalTarget(2, MemberStatus.PENDING, emptySet())))
        service.delete(listOf(2), 99)
        verify(members).softDeleteMembers(listOf(2))
    }

    @Test
    fun `기수 잠금을 회원 잠금보다 먼저 잡는다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet())))
        service.delete(listOf(1), 99)
        val order = inOrder(activities, members)
        order.verify(activities).lockCohortChanges()
        order.verify(members).lockApprovalTargets(listOf(1))
    }

    private companion object {
        val ACTIVE_COHORT = setOf(19L)
    }
}

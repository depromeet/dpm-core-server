package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

class MemberApprovalWelcomeServiceTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val service = MemberApprovalWelcomeService(members)

    @Test
    fun `승인 회원은 반복 확인할 수 있고 활동 종료 후에도 확인할 수 있다`() {
        listOf(MemberStatus.ACTIVE, MemberStatus.INACTIVE).forEach { status ->
            `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, status, setOf(19))))
            service.confirm(1)
            service.confirm(1)
        }
        verify(members, times(4)).markApprovalWelcomeSeen(1)
    }

    @Test
    fun `대기 및 반려 회원은 승인 화면 확인을 저장하지 않는다`() {
        listOf(MemberStatus.PENDING, MemberStatus.REJECTED).forEach { status ->
            `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, status, setOf(19))))
            assertThatThrownBy { service.confirm(1) }.isInstanceOfSatisfying(BusinessException::class.java) {
                assertThat(it.getCode()).isEqualTo(MemberExceptionCode.MEMBER_APPROVAL_WELCOME_NOT_ALLOWED)
            }
        }
        verify(members, times(2)).lockApprovalTargets(listOf(1))
        verifyNoMoreInteractions(members)
    }

    @Test
    fun `누락 탈퇴 삭제 회원은 기존 회원 예외로 거절한다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(emptyList())
        assertThatThrownBy { service.confirm(1) }.isInstanceOf(MemberNotFoundException::class.java)
        listOf(
            MemberApprovalTarget(1, MemberStatus.WITHDRAWN, emptySet()),
            MemberApprovalTarget(1, MemberStatus.ACTIVE, setOf(19), isDeleted = true),
        ).forEach {
            `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(it))
            assertThatThrownBy { service.confirm(1) }.isInstanceOf(MemberDeletedException::class.java)
        }
        verify(members, times(3)).lockApprovalTargets(listOf(1))
        verifyNoMoreInteractions(members)
    }
}

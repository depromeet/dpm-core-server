package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

class MemberDeletionServiceTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val service = MemberDeletionService(members)

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
            listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, emptySet())),
            listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, emptySet()), MemberApprovalTarget(2, MemberStatus.ACTIVE, emptySet(), true)),
            listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, emptySet()), MemberApprovalTarget(2, MemberStatus.WITHDRAWN, emptySet())),
        ).forEach { targets ->
            `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(targets)
            assertThatThrownBy { service.delete(listOf(2, 1), 99) }.isInstanceOf(MemberNotFoundException::class.java)
        }
        verify(members, org.mockito.Mockito.times(4)).lockApprovalTargets(listOf(1, 2))
        verifyNoMoreInteractions(members)
    }

    @Test
    fun `기수와 관계없이 미삭제 상태를 잠근 뒤 상태와 삭제시각만 변경한다`() {
        val statuses = MemberStatus.entries.filter { it != MemberStatus.WITHDRAWN }
        val ids = statuses.indices.map { it.toLong() + 1 }
        `when`(members.lockApprovalTargets(ids)).thenReturn(statuses.mapIndexed { index, status -> MemberApprovalTarget(index.toLong() + 1, status, emptySet()) })
        service.delete(ids.reversed(), 99)
        verify(members).lockApprovalTargets(ids)
        verify(members).softDeleteMembers(ids)
        verifyNoMoreInteractions(members)
    }
}

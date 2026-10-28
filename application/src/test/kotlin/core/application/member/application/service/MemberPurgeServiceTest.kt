package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberSharedRecordsException
import core.application.member.application.service.access.MemberAccessService
import core.domain.authorization.vo.RoleType
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.vo.MemberId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher

class MemberPurgeServiceTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val access = mock(MemberAccessService::class.java)
    private val events = mock(ApplicationEventPublisher::class.java)
    private val service = MemberPurgeService(members, access, events)

    @Test
    fun `MASTER가 아니면 조회 전에 거절한다`() {
        listOf(RoleType.Core, RoleType.Organizer, RoleType.Deeper, RoleType.Guest).forEach {
            `when`(access.getRoleType(MemberId(99))).thenReturn(it)
            assertThatThrownBy { service.purge(1, 99) }
                .isInstanceOf(BusinessException::class.java)
                .hasMessage(MemberExceptionCode.MEMBER_PURGE_MASTER_ONLY.message)
        }
        verifyNoInteractions(members, events)
    }

    @Test
    fun `0 이하 ID와 본인 계정은 삭제하지 않는다`() {
        `when`(access.getRoleType(MemberId(99))).thenReturn(RoleType.Master)
        listOf(0L, -1L).forEach {
            assertThatThrownBy { service.purge(it, 99) }.isInstanceOf(BusinessException::class.java)
        }
        assertThatThrownBy { service.purge(99, 99) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessage(MemberExceptionCode.MEMBER_SELF_DELETION_NOT_ALLOWED.message)
        verifyNoInteractions(members, events)
    }

    @Test
    fun `없는 회원이면 404이고 삭제하지 않는다`() {
        `when`(access.getRoleType(MemberId(99))).thenReturn(RoleType.Master)
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(emptyList())
        assertThatThrownBy { service.purge(1, 99) }.isInstanceOf(MemberNotFoundException::class.java)
        verify(members).lockApprovalTargets(listOf(1))
        verifyNoMoreInteractions(members)
        verifyNoInteractions(events)
    }

    @Test
    fun `소프트 삭제된 회원도 잠근 뒤 영구 삭제하고 이미지 키를 이벤트로 넘긴다`() {
        `when`(access.getRoleType(MemberId(99))).thenReturn(RoleType.Master)
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, emptySet(), true)))
        `when`(members.hardDeleteById(MemberId(1))).thenReturn(listOf("images/a"))
        service.purge(1, 99)
        verify(members).lockApprovalTargets(listOf(1))
        verify(members).findSharedRecordOwnerIds(listOf(1))
        verify(members).hardDeleteById(MemberId(1))
        verifyNoMoreInteractions(members)
        verify(events).publishEvent(MemberPurgedEvent(1, listOf("images/a")))
    }

    @Test
    fun `MASTER도 공유 기록 소유자를 영구 삭제할 수 없다`() {
        `when`(access.getRoleType(MemberId(99))).thenReturn(RoleType.Master)
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, emptySet())))
        `when`(members.findSharedRecordOwnerIds(listOf(1))).thenReturn(listOf(1L))
        val exception = assertThrows<MemberSharedRecordsException> { service.purge(1, 99) }
        assertThat(exception.blockedMemberIds).containsExactly(1)
        verify(members).lockApprovalTargets(listOf(1))
        verify(members).findSharedRecordOwnerIds(listOf(1))
        verifyNoMoreInteractions(members)
        verifyNoInteractions(events)
    }
}

package core.application.member.application.service

import core.application.member.application.exception.InvalidMemberMergeException
import core.application.member.application.exception.MemberOAuthConflictException
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.MemberStatus
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberMergePersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class MemberMergeServiceTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val merge = mock(MemberMergePersistencePort::class.java)
    private val cohorts = mock(CohortQueryUseCase::class.java)
    private val approval = mock(MemberApprovalService::class.java)
    private val service = MemberMergeService(members, merge, cohorts, approval)

    @BeforeEach
    fun setup() {
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(19))
        `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(
            listOf(
                MemberApprovalTarget(1, MemberStatus.PENDING, emptySet()),
                MemberApprovalTarget(2, MemberStatus.PENDING, setOf(19)),
            ),
        )
        `when`(merge.lockOAuths(listOf(1, 2))).thenReturn(listOf(oauth(1, OAuthProvider.KAKAO), oauth(2, OAuthProvider.APPLE)))
    }

    @Test
    fun `잠금 후 이전과 원본 보존 삭제와 유지 계정 승인을 실행한다`() {
        service.mergeAndApprove(2, 1)
        val order = inOrder(members, merge, approval)
        order.verify(members).lockApprovalTargets(listOf(1, 2))
        order.verify(merge).hasPasswordCredential(listOf(1, 2))
        order.verify(merge).lockOAuths(listOf(1, 2))
        order.verify(merge).transferOAuths(1, 2)
        order.verify(merge).softDeleteSource(1)
        order.verify(approval).approve(listOf(2))
    }

    @Test
    fun `비밀번호 또는 소셜 없는 대상은 이전하지 않는다`() {
        `when`(merge.hasPasswordCredential(listOf(1, 2))).thenReturn(true)
        assertThatThrownBy { service.mergeAndApprove(1, 2) }.isInstanceOf(InvalidMemberMergeException::class.java)
        verify(merge, never()).transferOAuths(2, 1)
        verifyNoInteractions(approval)
    }

    @Test
    fun `같은 provider의 다른 계정은 덮어쓰지 않는다`() {
        `when`(merge.lockOAuths(listOf(1, 2))).thenReturn(listOf(oauth(1, OAuthProvider.KAKAO), oauth(2, OAuthProvider.KAKAO)))
        assertThatThrownBy { service.mergeAndApprove(1, 2) }.isInstanceOf(MemberOAuthConflictException::class.java)
        verify(merge, never()).transferOAuths(2, 1)
        verifyNoInteractions(approval)
    }

    @Test
    fun `잠금 뒤 이미 승인된 계정과 삭제된 계정을 거절한다`() {
        listOf(
            MemberApprovalTarget(2, MemberStatus.ACTIVE, setOf(19)),
            MemberApprovalTarget(2, MemberStatus.PENDING, setOf(19), true),
            MemberApprovalTarget(2, MemberStatus.PENDING, setOf(18)),
        ).forEach { target ->
            `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet()), target))
            assertThatThrownBy { service.mergeAndApprove(1, 2) }.isInstanceOf(InvalidMemberMergeException::class.java)
        }
        verifyNoInteractions(merge, approval)
    }

    @Test
    fun `동일 식별자와 잘못된 ID는 조회하지 않는다`() {
        assertThatThrownBy { service.mergeAndApprove(1, 1) }.isInstanceOf(InvalidMemberMergeException::class.java)
        assertThatThrownBy { service.mergeAndApprove(null, 2) }.isInstanceOf(InvalidMemberMergeException::class.java)
        assertThatThrownBy { service.mergeAndApprove(1, 0) }.isInstanceOf(InvalidMemberMergeException::class.java)
        verifyNoInteractions(members, merge, approval)
    }

    private fun oauth(
        id: Long,
        provider: OAuthProvider,
    ) = MemberOAuth(MemberOAuthId(id), "external-$id", provider, MemberId(id), null)
}

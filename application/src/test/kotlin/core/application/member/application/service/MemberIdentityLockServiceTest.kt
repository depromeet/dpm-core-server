package core.application.member.application.service

import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberOAuthConflictException
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.MemberStatus
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberMergePersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class MemberIdentityLockServiceTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val oauths = mock(MemberMergePersistencePort::class.java)
    private val service = MemberIdentityLockService(members, oauths)
    private val observed = MemberOAuth(MemberOAuthId(10), "external-1", OAuthProvider.KAKAO, MemberId(1), null)

    @Test
    fun `통합 전에 읽었던 source 로그인은 잠금 이후 삭제 상태를 보고 실패한다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.WITHDRAWN, emptySet(), true)))
        assertThatThrownBy { service.lockOAuth(observed) }.isInstanceOf(MemberDeletedException::class.java)
        verifyNoInteractions(oauths)
    }

    @Test
    fun `OAuth 소유자가 바뀌었으면 오래된 관찰값으로 로그인하지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(listOf(MemberApprovalTarget(1, MemberStatus.PENDING, emptySet())))
        `when`(oauths.lockOAuths(listOf(1))).thenReturn(emptyList())
        assertThatThrownBy { service.lockOAuth(observed) }.isInstanceOf(MemberOAuthConflictException::class.java)
    }

    @Test
    fun `회원 행이 없는 OAuth는 연결 잠금과 소유자 검증 후 복구할 수 있다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(emptyList())
        `when`(oauths.lockOAuths(listOf(1))).thenReturn(listOf(observed))
        service.lockOAuth(observed)
        verify(oauths).lockOAuths(listOf(1))
    }

    @Test
    fun `고아 OAuth도 잠금 후 연결이 이동했으면 복구하지 않는다`() {
        `when`(members.lockApprovalTargets(listOf(1))).thenReturn(emptyList())
        `when`(oauths.lockOAuths(listOf(1))).thenReturn(emptyList())
        assertThatThrownBy { service.lockOAuth(observed) }.isInstanceOf(MemberOAuthConflictException::class.java)
    }
}

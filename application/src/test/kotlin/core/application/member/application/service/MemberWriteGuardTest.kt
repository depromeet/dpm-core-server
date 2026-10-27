package core.application.member.application.service

import core.application.member.application.exception.MemberNotFoundException
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class MemberWriteGuardTest {
    private val members = mock(MemberPersistencePort::class.java)
    private val guard = MemberWriteGuard(members)

    @Test
    fun `여러 주최자는 중복 제거 후 같은 순서로 잠근다`() {
        `when`(members.lockApprovalTargets(listOf(1, 2))).thenReturn(
            listOf(
                MemberApprovalTarget(1, MemberStatus.ACTIVE, emptySet()),
                MemberApprovalTarget(2, MemberStatus.ACTIVE, emptySet()),
            ),
        )
        guard.lockAvailable(listOf(2, 1, 2))
        verify(members).lockApprovalTargets(listOf(1, 2))
    }

    @Test
    fun `삭제 탈퇴 및 없는 회원은 공유 기록을 만들 수 없다`() {
        listOf(
            emptyList(),
            listOf(MemberApprovalTarget(1, MemberStatus.WITHDRAWN, emptySet())),
            listOf(MemberApprovalTarget(1, MemberStatus.ACTIVE, emptySet(), true)),
        ).forEach {
            `when`(members.lockApprovalTargets(listOf(1))).thenReturn(it)
            assertThatThrownBy { guard.lockAvailable(listOf(1)) }.isInstanceOf(MemberNotFoundException::class.java)
        }
    }
}

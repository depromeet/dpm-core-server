package core.application.member.application.service.access

import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.domain.authorization.vo.RoleType
import core.domain.member.vo.MemberId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class MemberAccessServiceTest {
    private val resolver = mock(CurrentCohortRoleResolver::class.java)
    private val service = MemberAccessService(resolver)

    @Test
    fun `단건과 일괄 조회는 코어 운영진 마스터를 관리자로 판정한다`() {
        val cases = listOf(RoleType.Core to true, RoleType.Organizer to true, RoleType.Master to true, RoleType.Deeper to false, RoleType.Guest to false)
        val expected = cases.mapIndexed { index, (role, admin) ->
            val id = MemberId(index.toLong() + 1)
            `when`(resolver.findPrimaryRoleTypeForMember(id)).thenReturn(role)
            assertEquals(admin, service.isAdmin(id))
            id to admin
        }.toMap()
        assertEquals(expected, service.getIsAdminByMemberIds(expected.keys.toList()))
        assertEquals(emptyMap<MemberId, Boolean>(), service.getIsAdminByMemberIds(emptyList()))
    }
}

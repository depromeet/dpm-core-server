package core.application.member.application.service.access

import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.domain.authorization.vo.RoleType
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberAccessService(
    private val currentCohortRoleResolver: CurrentCohortRoleResolver,
) {
    fun isAdmin(memberId: MemberId): Boolean = getRoleType(memberId) == RoleType.Organizer

    fun getRoleType(memberId: MemberId): RoleType =
        currentCohortRoleResolver.findPrimaryRoleTypeForMember(memberId)

    fun getIsAdminByMemberIds(memberIds: List<MemberId>): Map<MemberId, Boolean> =
        memberIds.associateWith { memberId -> getRoleType(memberId) == RoleType.Organizer }
}

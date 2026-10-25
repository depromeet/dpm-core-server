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
    fun isAdmin(memberId: MemberId): Boolean = getRoleType(memberId) in ADMIN_ROLES

    fun getRoleType(memberId: MemberId): RoleType = currentCohortRoleResolver.findPrimaryRoleTypeForMember(memberId)

    fun getIsAdminByMemberIds(memberIds: List<MemberId>): Map<MemberId, Boolean> =
        currentCohortRoleResolver.findPrimaryRoleTypesForMembers(
            memberIds,
        ).mapValues { (_, role) -> role in ADMIN_ROLES }

    companion object {
        private val ADMIN_ROLES = setOf(RoleType.Core, RoleType.Organizer, RoleType.Master)
    }
}

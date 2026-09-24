package core.application.authorization.application.service

import core.application.authorization.presentation.request.UpdateMemberRoleRequest
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.role.MemberRoleService
import core.domain.authorization.vo.RoleType
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RoleCommandService(
    private val memberQueryService: MemberQueryService,
    private val memberRoleService: MemberRoleService,
) {
    fun updateMemberRole(
        memberId: MemberId,
        request: UpdateMemberRoleRequest,
    ) {
        memberQueryService.getMemberById(memberId)
        val roleType = RoleType.fromCode(request.roleType)
        require(roleType == RoleType.Core || roleType == RoleType.Organizer || roleType == RoleType.Deeper) {
            "roleType must be CORE, ORGANIZER or DEEPER"
        }
        memberRoleService.replaceCohortRole(memberId, roleType, CohortId(request.cohortId))
    }
}

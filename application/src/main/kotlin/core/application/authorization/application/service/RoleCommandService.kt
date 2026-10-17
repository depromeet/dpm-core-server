package core.application.authorization.application.service

import core.application.authorization.presentation.request.UpdateMemberRoleRequest
import core.application.cohort.application.service.CohortQueryService
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.TrackMemberBadges
import core.application.member.application.service.role.MemberRoleService
import core.domain.authorization.vo.RoleType
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
class RoleCommandService(
    private val memberQueryService: MemberQueryService,
    private val memberRoleService: MemberRoleService,
    private val cohortQueryService: CohortQueryService,
) {
    @TrackMemberBadges
    fun updateMemberRole(
        memberId: MemberId,
        request: UpdateMemberRoleRequest,
    ) {
        memberQueryService.getMemberById(memberId)
        val roleType = RoleType.fromCode(request.roleType)
        require(roleType == RoleType.Core || roleType == RoleType.Organizer || roleType == RoleType.Deeper) {
            "roleType must be CORE, ORGANIZER or DEEPER"
        }
        val cohort = cohortQueryService.getCohortByValue(request.cohort)
        val cohortId = cohort.id ?: throw IllegalStateException("cohort id is null: ${request.cohort}")
        memberRoleService.replaceCohortRole(memberId, roleType, cohortId)
    }
}

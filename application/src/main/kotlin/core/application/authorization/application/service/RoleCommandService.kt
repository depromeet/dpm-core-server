package core.application.authorization.application.service

import core.application.cohort.application.service.CohortQueryService
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.authority.MemberAuthorityService
import core.application.member.application.service.role.MemberRoleService
import core.application.member.presentation.request.UpdateMemberRoleRequest
import core.domain.authorization.vo.RoleType
import core.domain.member.constant.AuthorityConstants.DEEPER_AUTHORITY_ID
import core.domain.member.constant.AuthorityConstants.ORGANIZER_AUTHORITY_ID
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RoleCommandService(
    private val cohortQueryService: CohortQueryService,
    private val memberQueryService: MemberQueryService,
    private val memberRoleService: MemberRoleService,
    private val memberAuthorityService: MemberAuthorityService,
) {
    fun updateMemberRole(
        memberId: MemberId,
        request: UpdateMemberRoleRequest,
    ) {
        memberQueryService.getMemberById(memberId)

        val cohortValue = normalizeCohortValue(request.cohort)
        val roleType =
            if (request.isAdmin) {
                RoleType.Organizer
            } else {
                RoleType.Deeper
            }
        val roleName = "${cohortValue}기 ${roleType.aliases.first()}"

        memberRoleService.replaceCohortRoleByName(
            memberId = memberId,
            roleName = roleName,
            cohortRolePrefix = "${cohortValue}기 ",
        )

        if (cohortValue == cohortQueryService.getLatestCohortValue()) {
            syncLegacyAuthority(memberId, request.isAdmin)
        }
    }

    private fun syncLegacyAuthority(
        memberId: MemberId,
        isAdmin: Boolean,
    ) {
        if (isAdmin) {
            memberAuthorityService.ensureAuthorityAssigned(memberId, ORGANIZER_AUTHORITY_ID)
            memberAuthorityService.revokeAuthority(memberId, DEEPER_AUTHORITY_ID)
        } else {
            memberAuthorityService.ensureAuthorityAssigned(memberId, DEEPER_AUTHORITY_ID)
            memberAuthorityService.revokeAuthority(memberId, ORGANIZER_AUTHORITY_ID)
        }
    }

    private fun normalizeCohortValue(cohort: String): String = cohort.trim().removeSuffix("기")
}

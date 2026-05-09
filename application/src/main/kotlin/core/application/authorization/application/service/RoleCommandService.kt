package core.application.authorization.application.service

import core.application.authorization.presentation.request.UpdateMemberRoleRequest
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.role.MemberRoleService
import core.domain.authorization.vo.RoleType
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

        val cohortValue = normalizeCohortValue(request.cohort)
        require(cohortValue.all(Char::isDigit)) {
            "cohort must be numeric (e.g. 17 or 17기)"
        }
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
    }

    private fun normalizeCohortValue(cohort: String): String = cohort.trim().removeSuffix("기")
}

package core.application.member.application.service.access

import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.authorization.vo.RoleType
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberAccessService(
    private val memberRolePersistencePort: MemberRolePersistencePort,
    private val roleQueryUseCase: RoleQueryUseCase,
    private val cohortQueryUseCase: CohortQueryUseCase,
    private val currentCohortRoleResolver: CurrentCohortRoleResolver,
) {
    fun isAdmin(memberId: MemberId): Boolean =
        getRoleType(
            memberId = memberId,
            cohortValue = cohortQueryUseCase.getLatestCohortValue(),
        ) == RoleType.Organizer

    fun isAdmin(
        memberId: MemberId,
        cohortValue: String,
    ): Boolean = getRoleType(memberId, cohortValue) == RoleType.Organizer

    fun getRoleType(
        memberId: MemberId,
        cohortValue: String = cohortQueryUseCase.getLatestCohortValue(),
    ): RoleType {
        val currentRoles =
            currentCohortRoleResolver.filterCurrentRoles(
                roleNames = memberRolePersistencePort.findRoleNamesByMemberId(memberId.value),
                latestCohortValue = normalizeCohortValue(cohortValue),
            )

        return ROLE_PRIORITY.firstOrNull { roleType ->
            currentRoles.any { roleName -> RoleType.from(roleName) == roleType }
        } ?: RoleType.Guest
    }

    fun getEffectivePermissions(memberId: MemberId): List<String> = roleQueryUseCase.getPermissionsByMemberId(memberId)

    private fun normalizeCohortValue(cohortValue: String): String = cohortValue.trim().removeSuffix("기")

    companion object {
        private val ROLE_PRIORITY = listOf(RoleType.Core, RoleType.Organizer, RoleType.Deeper, RoleType.Guest)
    }
}

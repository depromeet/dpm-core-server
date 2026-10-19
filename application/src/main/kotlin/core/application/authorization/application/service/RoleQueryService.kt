package core.application.authorization.application.service

import core.application.cohort.application.service.CohortQueryService
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.domain.authorization.aggregate.Role
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.authorization.port.outbound.RolePersistencePort
import core.domain.authorization.vo.RoleDisplayName
import core.domain.authorization.vo.RoleType
import core.domain.cohort.vo.CohortId
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class RoleQueryService(
    private val rolePersistencePort: RolePersistencePort,
    private val memberRolePersistencePort: MemberRolePersistencePort,
    private val currentCohortRoleResolver: CurrentCohortRoleResolver,
    private val cohortQueryService: CohortQueryService,
) : RoleQueryUseCase {
    override fun getAllRoles(): List<Role> = getRolesByCohort("")

    override fun getRolesByCohort(cohort: String): List<Role> {
        val canonicalNames = setOf(RoleType.Core.code, RoleType.Organizer.code, RoleType.Deeper.code, RoleType.Guest.code)
        return rolePersistencePort.findAll().filter { it.name in canonicalNames }
    }

    override fun getRoleNamesByMemberId(memberId: MemberId): List<String> =
        memberRolePersistencePort
            .findActiveRoleAssignmentsByMemberId(memberId.value)
            .map { assignment -> RoleDisplayName.of(assignment.roleName, resolveCohortValue(assignment.cohortId)) }
            .distinct()

    override fun getRoleNamesByMemberIds(memberIds: List<MemberId>): Map<MemberId, List<String>> =
        memberIds.associateWith { memberId -> getRoleNamesByMemberId(memberId) }

    private fun resolveCohortValue(cohortId: CohortId?): Long? =
        cohortId?.value?.let { runCatching { cohortQueryService.getCohort(cohortId).value.toLong() }.getOrNull() }

    override fun getRolesByExternalId(externalId: String): List<String> =
        rolePersistencePort.findAllByMemberExternalId(externalId).ifEmpty { listOf(RoleType.Guest.code) }

    override fun getPermissionsByMemberId(memberId: MemberId): List<String> {
        val effectiveRoleNames = currentCohortRoleResolver.filterEffectiveRolesForMember(memberId)
        return rolePersistencePort.findAllPermissionsByMemberIdAndRoleNames(
            memberId = memberId,
            roleNames = effectiveRoleNames,
        )
    }

    override fun findIdByName(roleName: String): Long = rolePersistencePort.findIdByName(roleName)
}

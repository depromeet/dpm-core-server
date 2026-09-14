package core.application.member.application.service.role

import core.application.cohort.application.service.CohortQueryService
import core.domain.authorization.vo.RoleType
import core.domain.member.port.outbound.MemberCohortPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberRoleAssignment
import org.springframework.stereotype.Component

@Component
class CurrentCohortRoleResolver(
    private val cohortQueryService: CohortQueryService,
    private val memberCohortPersistencePort: MemberCohortPersistencePort,
    private val memberRolePersistencePort: MemberRolePersistencePort,
) {
    fun filterEffectiveRolesForMember(memberId: MemberId): List<String> =
        filterEffectiveRoles(
            assignments = memberRolePersistencePort.findActiveRoleAssignmentsByMemberId(memberId.value),
            context = buildContext(memberId),
        )

    fun findPrimaryRoleTypeForMember(memberId: MemberId): RoleType =
        findPrimaryRoleType(
            assignments = memberRolePersistencePort.findActiveRoleAssignmentsByMemberId(memberId.value),
            context = buildContext(memberId),
        )

    fun selectRepresentativeRoleForMember(
        memberId: MemberId,
        roleNames: List<String>,
    ): String? {
        val assignments = memberRolePersistencePort.findActiveRoleAssignmentsByMemberId(memberId.value)
            .filter { it.roleName in roleNames }
        return selectRepresentativeRole(assignments, buildContext(memberId))
    }

    fun filterEffectiveRoles(
        assignments: List<MemberRoleAssignment>,
        context: CohortRoleContext,
    ): List<String> =
        assignments
            .filter { isAssignmentEffective(it, context) }
            .map { it.roleName }
            .distinct()

    fun findPrimaryRoleType(
        assignments: List<MemberRoleAssignment>,
        context: CohortRoleContext,
    ): RoleType {
        val effectiveRoles = filterEffectiveRoles(assignments, context)
        if (effectiveRoles.isEmpty()) return RoleType.Guest
        val roleTypes = effectiveRoles.map { RoleType.from(it) }
        return ROLE_PRIORITY.firstOrNull { it in roleTypes } ?: RoleType.Guest
    }

    fun selectRepresentativeRole(
        assignments: List<MemberRoleAssignment>,
        context: CohortRoleContext,
    ): String? =
        filterEffectiveRoles(assignments, context)
            .sortedWith(
                compareBy { roleName ->
                    ROLE_PRIORITY.indexOf(RoleType.from(roleName)).let { if (it == -1) Int.MAX_VALUE else it }
                },
            ).firstOrNull()

    private fun buildContext(memberId: MemberId): CohortRoleContext {
        val activeCohort = runCatching { cohortQueryService.getActiveCohort() }.getOrNull()
        return CohortRoleContext(
            activeCohortId = activeCohort?.id?.value,
            memberCohortIds = memberCohortPersistencePort.findCohortIdsByMemberId(memberId.value).toSet(),
        )
    }

    private fun isAssignmentEffective(
        assignment: MemberRoleAssignment,
        context: CohortRoleContext,
    ): Boolean {
        val isActiveMember = context.activeCohortId != null && context.activeCohortId in context.memberCohortIds
        return when (assignment.roleName) {
            RoleType.Master.code, RoleType.Guest.code -> true
            RoleType.Core.code, RoleType.Organizer.code, RoleType.Deeper.code -> {
                if (!isActiveMember) return false
                assignment.cohortId?.value?.let { it in context.memberCohortIds } ?: isActiveMember
            }
            else -> false
        }
    }

    data class CohortRoleContext(
        val activeCohortId: Long?,
        val memberCohortIds: Set<Long>,
    )

    companion object {
        private val ROLE_PRIORITY = listOf(RoleType.Master, RoleType.Core, RoleType.Organizer, RoleType.Deeper, RoleType.Guest)
    }
}

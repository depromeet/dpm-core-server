package core.application.authorization.application.service

import core.application.cohort.application.service.CohortQueryService
import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.domain.authorization.aggregate.Role
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.authorization.port.outbound.RolePersistencePort
import core.domain.authorization.vo.RoleType
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class RoleQueryService(
    private val rolePersistencePort: RolePersistencePort,
    private val memberRolePersistencePort: MemberRolePersistencePort,
    private val cohortQueryService: CohortQueryService,
    private val currentCohortRoleResolver: CurrentCohortRoleResolver,
) : RoleQueryUseCase {
    override fun getAllRoles(): List<Role> {
        val latestCohortValue = cohortQueryService.getLatestCohortValue()
        return rolePersistencePort
            .findAll()
            .filter { role ->
                val roleType = RoleType.from(role.name)
                when (roleType) {
                    RoleType.Deeper, RoleType.Organizer -> role.name.startsWith("${latestCohortValue}기 ")
                    else -> true
                }
            }
    }

    override fun getRolesByExternalId(externalId: String): List<String> =
        currentCohortRoleResolver
            .filterCurrentRoles(
                rolePersistencePort.findAllByMemberExternalId(externalId),
                cohortQueryService.getLatestCohortValue(),
            ).ifEmpty { listOf("GUEST") }

    override fun getPermissionsByMemberId(memberId: MemberId): List<String> {
        val currentRoleNames =
            currentCohortRoleResolver.filterCurrentRoles(
                memberRolePersistencePort.findRoleNamesByMemberId(memberId.value),
                cohortQueryService.getLatestCohortValue(),
            )
        return rolePersistencePort.findAllPermissionsByMemberIdAndRoleNames(
            memberId = memberId,
            roleNames = currentRoleNames,
        )
    }

    override fun findIdByName(roleName: String): Long = rolePersistencePort.findIdByName(roleName)
}

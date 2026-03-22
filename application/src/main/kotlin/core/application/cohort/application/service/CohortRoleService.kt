package core.application.cohort.application.service

import core.domain.authorization.port.outbound.RolePersistencePort
import core.domain.authorization.vo.RoleType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CohortRoleService(
    private val rolePersistencePort: RolePersistencePort,
    private val cohortQueryService: CohortQueryService,
) {
    fun createLatestCohortRoles(newCohortValue: String) {
        val sourceCohortValue = cohortQueryService.getPreviousNumericCohortValue(newCohortValue) ?: return

        listOf(RoleType.Deeper, RoleType.Organizer).forEach { roleType ->
            val sourceRoleName = buildCohortRoleName(sourceCohortValue, roleType)
            if (!rolePersistencePort.existsByName(sourceRoleName)) {
                return@forEach
            }

            val targetRoleName = buildCohortRoleName(newCohortValue, roleType)
            if (rolePersistencePort.existsByName(targetRoleName)) {
                return@forEach
            }

            val permissionIds = rolePersistencePort.findPermissionIdsByRoleName(sourceRoleName)
            rolePersistencePort.createRoleWithPermissions(
                roleName = targetRoleName,
                permissionIds = permissionIds,
            )
        }
    }

    fun buildCohortRoleName(
        cohortValue: String,
        roleType: RoleType,
    ): String = "${cohortValue}기 ${roleType.aliases.first()}"
}

package core.application.member.application.service.role

import core.domain.authorization.vo.RoleType
import org.springframework.stereotype.Component

@Component
class CurrentCohortRoleResolver {
    fun filterCurrentRoles(
        roleNames: List<String>,
        latestCohortValue: String,
    ): List<String> {
        val latestCohortNumber = latestCohortValue.toIntOrNull()
        return roleNames.filter { roleName ->
            val cohortRole = parseCohortRole(roleName)
            when {
                cohortRole == null -> true
                latestCohortNumber == null -> false
                else -> cohortRole.cohortNumber == latestCohortNumber
            }
        }
    }

    fun findPrimaryRoleType(
        roleNames: List<String>,
        latestCohortValue: String,
    ): RoleType {
        val currentRoles = filterCurrentRoles(roleNames, latestCohortValue)
        if (currentRoles.isEmpty()) {
            return RoleType.Guest
        }

        val roleTypes = currentRoles.map { RoleType.from(it) }
        return ROLE_PRIORITY.firstOrNull { it in roleTypes } ?: RoleType.Guest
    }

    fun selectRepresentativeRole(
        roleNames: List<String>,
        latestCohortValue: String,
    ): String? =
        filterCurrentRoles(roleNames, latestCohortValue)
            .sortedWith(
                compareBy<String> { roleName ->
                    ROLE_PRIORITY.indexOf(RoleType.from(roleName)).let { if (it == -1) Int.MAX_VALUE else it }
                }.thenByDescending { parseCohortRole(it)?.cohortNumber ?: Int.MIN_VALUE },
            ).firstOrNull()

    fun extractCurrentCohortValue(
        cohortValues: List<String>,
        latestCohortValue: String,
    ): String? =
        cohortValues.firstOrNull { it == latestCohortValue }
            ?: cohortValues.maxByOrNull { value ->
                value.toIntOrNull() ?: Int.MIN_VALUE
            }

    private fun parseCohortRole(roleName: String): ParsedCohortRole? {
        val match = COHORT_ROLE_REGEX.matchEntire(roleName.trim()) ?: return null
        val cohortNumber = match.groupValues[1].toIntOrNull() ?: return null
        return ParsedCohortRole(cohortNumber = cohortNumber)
    }

    private data class ParsedCohortRole(
        val cohortNumber: Int,
    )

    companion object {
        private val COHORT_ROLE_REGEX = Regex("^(\\d+)기\\s+.+$")
        private val ROLE_PRIORITY = listOf(RoleType.Core, RoleType.Organizer, RoleType.Deeper, RoleType.Guest)
    }
}

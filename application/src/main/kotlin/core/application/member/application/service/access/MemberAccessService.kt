package core.application.member.application.service.access

import core.application.member.application.service.role.CurrentCohortRoleResolver
import core.domain.authorization.vo.RoleType
import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberAccessService(
    private val memberRolePersistencePort: MemberRolePersistencePort,
    private val memberPersistencePort: MemberPersistencePort,
    private val currentCohortRoleResolver: CurrentCohortRoleResolver,
) {
    fun isAdmin(memberId: MemberId): Boolean = getRoleType(memberId) == RoleType.Organizer

    fun isAdmin(
        memberId: MemberId,
        cohortValue: String,
    ): Boolean = getRoleType(memberId, cohortValue) == RoleType.Organizer

    fun getRoleType(
        memberId: MemberId,
        cohortValue: String? = null,
    ): RoleType {
        val resolvedCohortValue = normalizeCohortValue(cohortValue ?: latestCohortValue(memberId))
        val currentRoles =
            currentCohortRoleResolver.filterCurrentRoles(
                roleNames = memberRolePersistencePort.findRoleNamesByMemberId(memberId.value),
                latestCohortValue = resolvedCohortValue,
            )

        return ROLE_PRIORITY.firstOrNull { roleType ->
            currentRoles.any { roleName -> RoleType.from(roleName) == roleType }
        } ?: RoleType.Guest
    }

    fun getIsAdminByMemberIds(
        memberIds: List<MemberId>,
    ): Map<MemberId, Boolean> {
        if (memberIds.isEmpty()) return emptyMap()

        val membersById: Map<MemberId, Member> =
            memberPersistencePort.findAllByIds(memberIds).associateBy { requireNotNull(it.id) }
        val roleNamesByMemberId: Map<Long, List<String>> =
            memberRolePersistencePort.findRoleNamesByMemberIds(memberIds.map { it.value })

        return memberIds.associateWith { memberId ->
            val member = membersById[memberId]
            val latestCohortValue = normalizeCohortValue(member?.latestCohortValue().orEmpty())
            val currentRoles =
                currentCohortRoleResolver.filterCurrentRoles(
                    roleNames = roleNamesByMemberId[memberId.value] ?: emptyList(),
                    latestCohortValue = latestCohortValue,
                )
            val roleType =
                ROLE_PRIORITY.firstOrNull { roleType ->
                    currentRoles.any { roleName -> RoleType.from(roleName) == roleType }
                } ?: RoleType.Guest

            roleType == RoleType.Organizer
        }
    }

    private fun latestCohortValue(memberId: MemberId): String =
        memberPersistencePort.findById(memberId)?.latestCohortValue().orEmpty()

    private fun normalizeCohortValue(cohortValue: String?): String = cohortValue.orEmpty().trim().removeSuffix("기")

    companion object {
        private val ROLE_PRIORITY = listOf(RoleType.Core, RoleType.Organizer, RoleType.Deeper, RoleType.Guest)
    }
}

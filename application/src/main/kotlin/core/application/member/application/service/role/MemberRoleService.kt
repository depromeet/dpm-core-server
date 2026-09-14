package core.application.member.application.service.role

import core.application.cohort.application.service.CohortQueryService
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.authorization.vo.RoleId
import core.domain.authorization.vo.RoleType
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.MemberRole
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MemberRoleService(
    private val memberRolePersistencePort: MemberRolePersistencePort,
    private val roleQueryUseCase: RoleQueryUseCase,
    private val cohortQueryService: CohortQueryService,
    private val currentCohortRoleResolver: CurrentCohortRoleResolver,
) {
    fun getRoleNamesByMemberId(memberId: MemberId): List<String> =
        memberRolePersistencePort.findRoleNamesByMemberId(memberId.value)

    fun revokeAllRoles(memberId: MemberId) = memberRolePersistencePort.softDeleteAllByMemberId(memberId.value)

    fun resolvePrimaryRoleType(memberId: MemberId): RoleType =
        currentCohortRoleResolver.findPrimaryRoleTypeForMember(memberId)

    fun assignGuestRole(memberId: MemberId) {
        val guestRoleId = roleQueryUseCase.findIdByName(RoleType.Guest.code)
        memberRolePersistencePort.save(
            MemberRole(memberId = memberId, roleId = RoleId(guestRoleId), grantedAt = Instant.now()),
        )
    }

    fun assignRole(memberId: MemberId, roleType: RoleType, cohortId: CohortId? = null) {
        val roleId = roleQueryUseCase.findIdByName(roleType.code)
        memberRolePersistencePort.save(
            MemberRole(
                memberId = memberId,
                roleId = RoleId(roleId),
                cohortId = cohortId,
                grantedAt = Instant.now(),
            ),
        )
    }

    fun ensureRoleAssigned(memberId: MemberId, roleType: RoleType) {
        val roles = memberRolePersistencePort.findRoleNamesByMemberId(memberId.value)
        if (roles.none { it == roleType.code }) assignRole(memberId, roleType)
    }

    fun revokeRole(memberId: MemberId, roleType: RoleType) {
        val roleId = roleQueryUseCase.findIdByName(roleType.code)
        memberRolePersistencePort.softDeleteByMemberIdAndRoleId(memberId.value, roleId)
    }

    fun ensureGuestRoleAssigned(memberId: MemberId) {
        if (memberRolePersistencePort.findRoleNamesByMemberId(memberId.value).isEmpty()) assignGuestRole(memberId)
    }

    fun replaceWithSingleRoleByType(memberId: MemberId, roleType: RoleType, cohortId: CohortId? = null) {
        val roleId = roleQueryUseCase.findIdByName(roleType.code)
        memberRolePersistencePort.upsertSingleActiveRole(
            memberId = memberId.value,
            roleId = roleId,
            cohortId = cohortId?.value,
        )
    }

    fun replaceCohortRole(memberId: MemberId, roleType: RoleType, cohortId: CohortId) {
        val roleId = roleQueryUseCase.findIdByName(roleType.code)
        memberRolePersistencePort.replaceCohortRole(memberId.value, roleId, cohortId.value)
    }
}

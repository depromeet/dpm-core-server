package core.application.member.application.service.role

import core.domain.authorization.vo.RoleType
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class MemberRoleService(
    private val memberRolePersistencePort: MemberRolePersistencePort,
) {
    /**
     * 멤버 식별자로 해당 멤버가 소유한 권한 이름 목록을 조회함.
     *
     * @author LeeHanEum
     * @since 2025.07.24
     */
    fun getRoleNamesByMemberId(memberId: MemberId): List<String> =
        memberRolePersistencePort
            .findRoleNamesByMemberId(memberId.value)

    /**
     * 멤버 탈퇴 시에, 멤버가 소유한 모든 권한을 Soft Delete 처리하여, 권한을 회수함.
     *
     *
     * @author LeeHanEum
     * @since 2025.09.01
     */
    fun revokeAllRoles(memberId: MemberId) =
        memberRolePersistencePort.softDeleteAllByMemberId(memberId.value)

    /**
     * 멤버 식별자로 해당 멤버의 최우선 권한 타입을 조회합니다.
     *
     * 권한 타입의 위계는 CORE > ORGANIZER > DEEPER > GUEST 순입니다.
     *
     * @author LeeHanEum
     * @since 2025.9.15
     */
    fun resolvePrimaryAuthorityType(memberId: MemberId): RoleType {
        val roles =
            memberRolePersistencePort
                .findRoleNamesByMemberId(memberId.value)

        if (roles.isEmpty()) {
            return RoleType.Guest
        }

        val roleTypes = roles.map { RoleType.from(it) }
        return ROLE_PRIORITY.firstOrNull { it in roleTypes }
            ?: RoleType.Guest
    }

    companion object {
        private val ROLE_PRIORITY: List<RoleType> = listOf(
            RoleType.Core,
            RoleType.Organizer,
            RoleType.Deeper,
        )
    }

}

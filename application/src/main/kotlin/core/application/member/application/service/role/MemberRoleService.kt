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
            .findAuthorityNamesByMemberId(memberId.value)

    /**
     * 멤버 탈퇴 시에, 멤버가 소유한 모든 권한을 Soft Delete 처리하여, 권한을 회수함.
     *
     *
     * @author LeeHanEum
     * @since 2025.09.01
     */
    fun revokeAllAuthorities(memberId: MemberId) =
        memberRolePersistencePort.softDeleteAllByMemberId(memberId.value)

    /**
     * 멤버 식별자로 해당 멤버의 최우선 권한 타입을 조회합니다.
     *
     * 권한 타입의 위계는 ORGANIZER > DEEPER > GUEST 순입니다.
     *
     * @author LeeHanEum
     * @since 2025.9.15
     */
    fun resolvePrimaryAuthorityType(memberId: MemberId): RoleType {
        val authorities =
            memberRolePersistencePort
                .findAuthorityNamesByMemberId(memberId.value)

        if (authorities.isEmpty()) {
            return RoleType.Guest
        }

        val priority = listOf(
            RoleType.Core,
            RoleType.Organizer,
            RoleType.Deeper,
        )

        return priority.firstOrNull { roleType ->
            authorities.any { authority ->
                roleType.matches(authority)
            }
        } ?: RoleType.Guest
    }

}

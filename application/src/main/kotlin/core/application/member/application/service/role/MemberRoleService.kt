package core.application.member.application.service.role

import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.authorization.vo.RoleType
import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberRole
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import core.domain.security.oauth.dto.OAuthAttributes
import org.springframework.stereotype.Service

@Service
class MemberRoleService(
    private val memberRolePersistencePort: MemberRolePersistencePort,
    private val roleQueryUseCase: RoleQueryUseCase,
) {
    /**
     * 멤버 식별자로 해당 멤버가 소유한 권한 이름 목록을 조회함.
     *
     * @author LeeHanEum
     * @since 2025.12.27
     */
    fun getRoleNamesByMemberId(memberId: MemberId): List<String> =
        memberRolePersistencePort
            .findRoleNamesByMemberId(memberId.value)

    /**
     * 멤버 탈퇴 시에, 멤버가 소유한 모든 권한을 Soft Delete 처리하여, 권한을 회수함.
     *
     *
     * @author LeeHanEum
     * @since 2025.12.27
     */
    fun revokeAllRoles(memberId: MemberId) = memberRolePersistencePort.softDeleteAllByMemberId(memberId.value)

    /**
     * 멤버 식별자로 해당 멤버의 최우선 권한 타입을 조회합니다.
     *
     * 권한 타입의 위계는 CORE > ORGANIZER > DEEPER > GUEST 순입니다.
     *
     * @author LeeHanEum
     * @since 2025.12.27
     */
    fun resolvePrimaryRoleType(memberId: MemberId): RoleType {
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

    fun saveByMemberId(
        member: Member,
        authAttributes: OAuthAttributes,
    ) {
        val guestRoleId = roleQueryUseCase.getRoleIdByCode(RoleType.Guest.code)
        val memberRole =
            MemberRole.of(
                memberId = member.id!!,
                roleId = guestRoleId,
            )
        memberRolePersistencePort.save(memberRole)
    }

    companion object {
        private val ROLE_PRIORITY: List<RoleType> =
            listOf(
                RoleType.Core,
                RoleType.Organizer,
                RoleType.Deeper,
            )
    }
}

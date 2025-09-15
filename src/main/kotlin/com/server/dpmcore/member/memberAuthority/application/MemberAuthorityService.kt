package com.server.dpmcore.member.memberAuthority.application

import com.server.dpmcore.authority.domain.model.AuthorityType
import com.server.dpmcore.authority.domain.port.inbound.AuthorityQueryUseCase
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.memberAuthority.domain.model.MemberAuthority
import com.server.dpmcore.member.memberAuthority.domain.port.outbound.MemberAuthorityPersistencePort
import org.springframework.stereotype.Service

@Service
class MemberAuthorityService(
    private val memberAuthorityPersistencePort: MemberAuthorityPersistencePort,
    private val authorityQueryUseCase: AuthorityQueryUseCase,
) {
    /**
     * 멤버 식별자로 해당 멤버가 소유한 권한 이름 목록을 조회함.
     *
     * @author LeeHanEum
     * @since 2025.07.24
     */
    fun getAuthorityNamesByMemberId(memberId: MemberId): List<String> =
        memberAuthorityPersistencePort
            .findAuthorityNamesByMemberId(memberId.value)

    /**
     * 권한 타입으로 권한 식별자를 조회하고, 해당 권한 식별자로 멤버 권한을 추가함.
     *
     * @author LeeHanEum
     * @since 2025.08.02
     */
    fun setMemberAuthorityByMemberId(
        memberId: MemberId,
        authorityType: AuthorityType,
    ) {
        val authorityId = authorityQueryUseCase.getAuthorityIdByType(authorityType)
        memberAuthorityPersistencePort.save(MemberAuthority.of(memberId, authorityId))
    }

    /**
     * 멤버 탈퇴 시에, 멤버가 소유한 모든 권한을 Soft Delete 처리하여, 권한을 회수함.
     *
     *
     * @author LeeHanEum
     * @since 2025.09.01
     */
    fun revokeAllAuthorities(memberId: MemberId) =
        memberAuthorityPersistencePort.softDeleteAllByMemberId(memberId.value)

    /**
     * 멤버 식별자로 해당 멤버의 최우선 권한 타입을 조회합니다.
     *
     * 권한 타입의 위계는 ORGANIZER > DEEPER > GUEST 순입니다.
     *
     * @author LeeHanEum
     * @since 2025.9.15
     */
    fun resolvePrimaryAuthorityType(memberId: MemberId): AuthorityType {
        val authorities =
            memberAuthorityPersistencePort
                .findAuthorityNamesByMemberId(memberId.value)

        return when {
            AuthorityType.ORGANIZER.name in authorities -> AuthorityType.ORGANIZER
            authorities.isNotEmpty() -> AuthorityType.valueOf(authorities.first())
            else -> AuthorityType.GUEST
        }
    }
}

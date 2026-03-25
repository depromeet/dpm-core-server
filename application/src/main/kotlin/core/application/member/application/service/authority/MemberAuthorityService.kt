package core.application.member.application.service.authority

import core.domain.member.constant.AuthorityConstants.ORGANIZER_AUTHORITY_ID
import core.domain.member.port.outbound.MemberAuthorityPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class MemberAuthorityService(
    private val memberAuthorityPersistencePort: MemberAuthorityPersistencePort,
) {
    fun getActiveAuthorityIdsByMemberId(memberId: MemberId): List<Long> =
        memberAuthorityPersistencePort.findActiveAuthorityIdsByMemberId(memberId)

    fun getAuthorityNamesByMemberId(memberId: MemberId): List<String> =
        memberAuthorityPersistencePort.findAuthorityNamesByMemberId(memberId)

    fun ensureAuthorityAssigned(
        memberId: MemberId,
        authorityName: String,
    ) = memberAuthorityPersistencePort.ensureAuthorityAssigned(memberId, authorityName)

    fun ensureAuthorityAssigned(
        memberId: MemberId,
        authorityId: Long,
    ) = memberAuthorityPersistencePort.ensureAuthorityAssigned(memberId, authorityId)

    fun revokeAuthority(
        memberId: MemberId,
        authorityName: String,
    ) = memberAuthorityPersistencePort.revokeAuthority(memberId, authorityName)

    fun revokeAuthority(
        memberId: MemberId,
        authorityId: Long,
    ) = memberAuthorityPersistencePort.revokeAuthority(memberId, authorityId)

    fun revokeAllAuthorities(memberId: MemberId) = memberAuthorityPersistencePort.revokeAllByMemberId(memberId)

    fun isAdmin(memberId: MemberId): Boolean =
        memberAuthorityPersistencePort.findActiveAuthorityIdsByMemberId(memberId).contains(ORGANIZER_AUTHORITY_ID)
}

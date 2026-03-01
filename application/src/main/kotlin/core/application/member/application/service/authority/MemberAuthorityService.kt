package core.application.member.application.service.authority

import core.domain.member.port.outbound.MemberAuthorityPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class MemberAuthorityService(
    private val memberAuthorityPersistencePort: MemberAuthorityPersistencePort,
) {
    fun ensureAuthorityAssigned(
        memberId: MemberId,
        authorityName: String,
    ) = memberAuthorityPersistencePort.ensureAuthorityAssigned(memberId, authorityName)

    fun revokeAuthority(
        memberId: MemberId,
        authorityName: String,
    ) = memberAuthorityPersistencePort.revokeAuthority(memberId, authorityName)
}

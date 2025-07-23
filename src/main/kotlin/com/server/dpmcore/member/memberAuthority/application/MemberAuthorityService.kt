package com.server.dpmcore.member.memberAuthority.application

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.memberAuthority.domain.port.outbound.MemberAuthorityPersistencePort
import org.springframework.stereotype.Service

@Service
class MemberAuthorityService(
    private val memberAuthorityPersistencePort: MemberAuthorityPersistencePort,
) {
    fun getAuthorityNamesByMemberId(memberId: MemberId): List<String> =
        memberAuthorityPersistencePort
            .findAuthorityNamesByMemberId(memberId.value)
}

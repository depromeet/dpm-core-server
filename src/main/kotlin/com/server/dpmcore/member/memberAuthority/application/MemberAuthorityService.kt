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
    fun getAuthorityNamesByMemberId(memberId: MemberId): List<String> =
        memberAuthorityPersistencePort
            .findAuthorityNamesByMemberId(memberId.value)

    fun setMemberAuthorityByMemberId(
        memberId: MemberId,
        authorityType: AuthorityType,
    ) {
        val authorityId = authorityQueryUseCase.getAuthorityIdByType(authorityType)
        memberAuthorityPersistencePort.save(MemberAuthority.of(memberId, authorityId))
    }
}

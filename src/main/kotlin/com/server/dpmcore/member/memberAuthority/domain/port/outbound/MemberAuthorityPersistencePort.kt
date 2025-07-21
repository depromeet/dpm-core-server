package com.server.dpmcore.member.memberAuthority.domain.port.outbound

interface MemberAuthorityPersistencePort {
    fun findAuthorityNamesByMemberId(memberId: Long): List<String>
}

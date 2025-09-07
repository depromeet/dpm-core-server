package com.server.dpmcore.member.memberAuthority.domain.port.outbound

import com.server.dpmcore.member.memberAuthority.domain.model.MemberAuthority

interface MemberAuthorityPersistencePort {
    fun save(memberAuthority: MemberAuthority)

    fun findAuthorityNamesByMemberId(memberId: Long): List<String>

    fun softDeleteAllByMemberId(memberId: Long)
}

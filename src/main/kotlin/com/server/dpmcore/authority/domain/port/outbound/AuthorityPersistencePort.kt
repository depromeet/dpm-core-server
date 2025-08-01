package com.server.dpmcore.authority.domain.port.outbound

import com.server.dpmcore.authority.domain.model.Authority
import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.member.member.domain.model.MemberId

interface AuthorityPersistencePort {
    fun findAll(): List<Authority>

    fun findAllByMemberExternalId(externalId: String): List<String>

    fun findAllByMemberId(memberId: MemberId): List<String>

    fun findAuthorityIdByName(authorityName: String): AuthorityId?
}

package com.server.dpmcore.authority.domain.port.outbound

import com.server.dpmcore.authority.domain.model.Authority

interface AuthorityPersistencePort {
    fun findAll(): List<Authority>

    fun findAllByMemberExternalId(externalId: String): List<String>
}

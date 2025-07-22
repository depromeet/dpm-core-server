package com.server.dpmcore.authority.domain.port

import com.server.dpmcore.authority.domain.model.Authority

interface AuthorityPersistencePort {
    fun findAll(): List<Authority>
}

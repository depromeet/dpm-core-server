package com.server.dpmcore.authority.domain.port.inbound

interface AuthorityQueryUseCase {
    fun getAuthoritiesByMember(externalId: String): List<String>
}

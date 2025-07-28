package com.server.dpmcore.authority.application

import com.server.dpmcore.authority.domain.port.inbound.AuthorityQueryUseCase
import com.server.dpmcore.authority.domain.port.outbound.AuthorityPersistencePort
import com.server.dpmcore.authority.presentation.response.AuthorityListResponse
import org.springframework.stereotype.Service

@Service
class AuthorityQueryService(
    private val authorityPersistencePort: AuthorityPersistencePort,
) : AuthorityQueryUseCase {
    fun getAllAuthorities(): AuthorityListResponse = AuthorityListResponse.from(authorityPersistencePort.findAll())

    override fun getAuthoritiesByMember(externalId: String): List<String> =
        authorityPersistencePort
            .findAllByMemberExternalId(externalId)
            .ifEmpty { listOf("GUEST") }
}

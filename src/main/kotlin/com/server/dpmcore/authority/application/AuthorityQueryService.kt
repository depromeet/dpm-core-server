package com.server.dpmcore.authority.application

import com.server.dpmcore.authority.domain.port.inbound.AuthorityQueryUseCase
import com.server.dpmcore.authority.domain.port.outbound.AuthorityPersistencePort
import com.server.dpmcore.authority.presentation.response.AuthorityListResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import org.springframework.stereotype.Service

@Service
class AuthorityQueryService(
    private val authorityPersistencePort: AuthorityPersistencePort,
) : AuthorityQueryUseCase {
    fun getAllAuthorities(): AuthorityListResponse = AuthorityListResponse.from(authorityPersistencePort.findAll())

    override fun getAuthoritiesByExternalId(externalId: String): List<String> =
        authorityPersistencePort
            .findAllByMemberExternalId(externalId)
            .ifEmpty { listOf("GUEST") }

    override fun getAuthoritiesByMemberId(memberId: MemberId): List<String> =
        authorityPersistencePort
            .findAllByMemberId(memberId)
            .ifEmpty { listOf("GUEST") }
}

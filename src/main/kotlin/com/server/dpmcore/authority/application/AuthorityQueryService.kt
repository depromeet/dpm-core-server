package com.server.dpmcore.authority.application

import com.server.dpmcore.authority.domain.port.AuthorityPersistencePort
import com.server.dpmcore.authority.presentation.response.AuthorityListResponse
import org.springframework.stereotype.Service

@Service
class AuthorityQueryService(
    private val authorityPersistencePort: AuthorityPersistencePort,
) {
    fun getAllAuthorities(): AuthorityListResponse = AuthorityListResponse.from(authorityPersistencePort.findAll())
}

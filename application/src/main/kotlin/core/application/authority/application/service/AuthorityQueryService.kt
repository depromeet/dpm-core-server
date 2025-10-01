package core.application.authority.application.service

import core.application.authority.application.exception.AuthorityNotFoundException
import core.application.authority.presentation.response.AuthorityListResponse
import core.domain.authority.enums.AuthorityType
import core.domain.authority.port.inbound.AuthorityQueryUseCase
import core.domain.authority.port.outbound.AuthorityPersistencePort
import core.domain.authority.vo.AuthorityId
import core.domain.member.vo.MemberId
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

    override fun getAuthorityIdByType(authorityType: AuthorityType): AuthorityId =
        authorityPersistencePort.findAuthorityIdByName(authorityType.toString())
            ?: throw AuthorityNotFoundException()
}

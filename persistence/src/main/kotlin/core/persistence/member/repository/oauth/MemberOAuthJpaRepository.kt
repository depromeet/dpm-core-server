package core.persistence.member.repository.oauth

import core.entity.member.MemberOAuthEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberOAuthJpaRepository : JpaRepository<MemberOAuthEntity, Long> {
    fun findAllByProvider(provider: String): List<MemberOAuthEntity>

    fun findByProviderAndExternalId(
        provider: String,
        externalId: String,
    ): MemberOAuthEntity?

    fun deleteAllByMemberId(memberId: Long)
}

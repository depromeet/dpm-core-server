package core.persistence.member.repository.oauth

import core.entity.member.MemberOAuthEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface MemberOAuthJpaRepository : JpaRepository<MemberOAuthEntity, Long> {
    fun findAllByProvider(provider: String): List<MemberOAuthEntity>

    fun findByProviderAndExternalId(
        provider: String,
        externalId: String,
    ): MemberOAuthEntity?

    fun deleteAllByMemberId(memberId: Long)

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update MemberOAuthEntity m set m.email = :email " +
            "where m.provider = :provider and m.externalId = :externalId " +
            "and (m.email is null or m.email <> :email)",
    )
    fun updateEmail(
        @Param("provider") provider: String,
        @Param("externalId") externalId: String,
        @Param("email") email: String,
    ): Int
}

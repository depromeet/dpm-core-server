package core.persistence.member.repository.oauth

import core.entity.member.MemberOAuthEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberOAuthJpaRepository : JpaRepository<MemberOAuthEntity, Long> {
    fun findByProviderAndExternalId(
        provider: String,
        externalId: String,
    ): MemberOAuthEntity?

    @Query("select memberOAuth from MemberOAuthEntity memberOAuth where memberOAuth.member.id = :memberId")
    fun findAllByMemberId(
        @Param("memberId") memberId: Long,
    ): List<MemberOAuthEntity>

    fun deleteAllByMemberId(memberId: Long)
}

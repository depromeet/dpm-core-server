package core.persistence.authority.repository

import core.domain.authority.aggregate.Authority
import core.domain.authority.port.outbound.AuthorityPersistencePort
import core.domain.authority.vo.AuthorityId
import core.domain.member.vo.MemberId
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.AUTHORITIES
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_AUTHORITIES
import org.jooq.dsl.tables.references.MEMBER_OAUTH
import org.springframework.stereotype.Repository

@Repository
class AuthorityRepository(
    private val authorityJpaRepository: AuthorityJpaRepository,
    private val dsl: DSLContext,
) : AuthorityPersistencePort {
    override fun findAll(): List<Authority> = authorityJpaRepository.findAll().mapNotNull { it.toDomain() }

    override fun findAllByMemberExternalId(externalId: String): List<String> =
        dsl
            .select(AUTHORITIES.NAME)
            .from(AUTHORITIES)
            .join(MEMBER_AUTHORITIES)
            .on(MEMBER_AUTHORITIES.AUTHORITY_ID.eq(AUTHORITIES.AUTHORITY_ID))
            .join(MEMBERS)
            .on(MEMBER_AUTHORITIES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(MEMBER_OAUTH)
            .on(MEMBER_OAUTH.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .where(MEMBER_OAUTH.EXTERNAL_ID.eq(externalId).and(MEMBER_AUTHORITIES.DELETED_AT.isNull))
            .fetch(AUTHORITIES.NAME)
            .filterNotNull()

    override fun findAllByMemberId(memberId: MemberId): List<String> =
        dsl
            .select(AUTHORITIES.NAME)
            .from(AUTHORITIES)
            .join(MEMBER_AUTHORITIES)
            .on(MEMBER_AUTHORITIES.AUTHORITY_ID.eq(AUTHORITIES.AUTHORITY_ID))
            .join(MEMBERS)
            .on(MEMBER_AUTHORITIES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .where(MEMBERS.MEMBER_ID.eq(memberId.value).and(MEMBER_AUTHORITIES.DELETED_AT.isNull))
            .fetch(AUTHORITIES.NAME)
            .filterNotNull()

    override fun findAuthorityIdByName(authorityName: String): AuthorityId? =
        dsl
            .select(AUTHORITIES.AUTHORITY_ID)
            .from(AUTHORITIES)
            .where(AUTHORITIES.NAME.eq(authorityName))
            .fetchOne()
            ?.get(AUTHORITIES.AUTHORITY_ID)
            ?.let(::AuthorityId)
}

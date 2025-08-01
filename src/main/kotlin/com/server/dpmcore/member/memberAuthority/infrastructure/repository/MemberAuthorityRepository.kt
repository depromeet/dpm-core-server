package com.server.dpmcore.member.memberAuthority.infrastructure.repository

import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.server.dpmcore.member.memberAuthority.domain.model.MemberAuthority
import com.server.dpmcore.member.memberAuthority.domain.port.outbound.MemberAuthorityPersistencePort
import org.jooq.DSLContext
import org.jooq.generated.tables.references.AUTHORITIES
import org.jooq.generated.tables.references.MEMBER_AUTHORITIES
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class MemberAuthorityRepository(
    private val memberAuthorityJpaRepository: MemberAuthorityJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
    private val dsl: DSLContext,
) : MemberAuthorityPersistencePort {
    override fun findAuthorityNamesByMemberId(memberId: Long): List<String> =
        dsl
            .select(AUTHORITIES.NAME)
            .from(MEMBER_AUTHORITIES)
            .join(AUTHORITIES)
            .on(MEMBER_AUTHORITIES.AUTHORITY_ID.eq(AUTHORITIES.AUTHORITY_ID))
            .where(
                MEMBER_AUTHORITIES.MEMBER_ID
                    .eq(memberId)
                    .and(MEMBER_AUTHORITIES.DELETED_AT.isNull()),
            ).fetch(AUTHORITIES.NAME)
            .filterNotNull()

    override fun save(memberAuthority: MemberAuthority) {
        dsl
            .insertInto(MEMBER_AUTHORITIES)
            .set(MEMBER_AUTHORITIES.MEMBER_ID, memberAuthority.memberId.value)
            .set(MEMBER_AUTHORITIES.AUTHORITY_ID, memberAuthority.authorityId.value)
            .set(
                MEMBER_AUTHORITIES.GRANTED_AT,
                memberAuthority.grantedAt
                    ?.atZone(ZoneId.systemDefault())
                    ?.toLocalDateTime()
                    ?: LocalDateTime.now(),
            ).execute()
    }
}

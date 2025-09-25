package core.persistence.member.repository.authority

import core.domain.member.aggregate.MemberAuthority
import core.domain.member.port.outbound.MemberAuthorityPersistencePort
import org.jooq.dsl.tables.references.AUTHORITIES
import org.jooq.dsl.tables.references.MEMBER_AUTHORITIES
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class MemberAuthorityRepository(
    private val dsl: DSLContext,
) : MemberAuthorityPersistencePort {
    override fun save(memberAuthority: MemberAuthority) {
        dsl
            .insertInto(MEMBER_AUTHORITIES)
            .set(MEMBER_AUTHORITIES.MEMBER_ID, memberAuthority.memberId.value)
            .set(MEMBER_AUTHORITIES.AUTHORITY_ID, memberAuthority.authorityId.value)
            .set(
                MEMBER_AUTHORITIES.GRANTED_AT,
                memberAuthority.grantedAt
                    ?.atZone(ZoneId.of(TIME_ZONE))
                    ?.toLocalDateTime()
                    ?: LocalDateTime.now(),
            ).execute()
    }

    /**
     * 단순 soft delete를 위해 jOOQ 사용.
     *
     * MemberAuthority의 연관관계가 복잡해 JPA로 save를 시도하면 불필요한 연관 엔티티 조회가 발생함.
     *
     * 이에 jOOQ로 직접 update 쿼리를 작성하여, 성능을 최적화하고 불필요한 JPA 연산을 최소화함.
     *
     * @author LeeHanEum
     * @since 2025.09.02
     */
    override fun softDeleteAllByMemberId(memberId: Long) {
        dsl
            .update(MEMBER_AUTHORITIES)
            .set(
                MEMBER_AUTHORITIES.DELETED_AT,
                LocalDateTime.now(ZoneId.of(TIME_ZONE)),
            ).where(
                MEMBER_AUTHORITIES.MEMBER_ID
                    .eq(memberId)
                    .and(MEMBER_AUTHORITIES.DELETED_AT.isNull()),
            ).execute()
    }

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

    companion object {
        private const val TIME_ZONE = "Asia/Seoul"
    }
}

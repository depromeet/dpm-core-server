package core.persistence.member.repository.role

import core.domain.member.aggregate.MemberRole
import core.domain.member.port.outbound.MemberRolePersistencePort
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.ROLES
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class MemberRoleRepository(
    private val dsl: DSLContext,
) : MemberRolePersistencePort {
    override fun save(memberRole: MemberRole) {
        dsl
            .insertInto(MEMBER_ROLES)
            .set(MEMBER_ROLES.MEMBER_ID, memberRole.memberId.value)
            .set(MEMBER_ROLES.ROLE_ID, memberRole.roleId.value)
            .set(
                MEMBER_ROLES.GRANTED_AT,
                memberRole.grantedAt
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
            .update(MEMBER_ROLES)
            .set(
                MEMBER_ROLES.DELETED_AT,
                LocalDateTime.now(ZoneId.of(TIME_ZONE)),
            ).where(
                MEMBER_ROLES.MEMBER_ID
                    .eq(memberId)
                    .and(MEMBER_ROLES.DELETED_AT.isNull()),
            ).execute()
    }

    override fun findAuthorityNamesByMemberId(memberId: Long): List<String> =
        dsl
            .select(ROLES.NAME)
            .from(MEMBER_ROLES)
            .join(ROLES)
            .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .where(
                MEMBER_ROLES.MEMBER_ID
                    .eq(memberId)
                    .and(MEMBER_ROLES.DELETED_AT.isNull()),
            ).fetch(ROLES.NAME)
            .filterNotNull()

    companion object {
        private const val TIME_ZONE = "Asia/Seoul"
    }
}

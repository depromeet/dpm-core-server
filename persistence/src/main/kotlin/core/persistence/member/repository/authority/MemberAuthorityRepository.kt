package core.persistence.member.repository.authority

import core.domain.member.port.outbound.MemberAuthorityPersistencePort
import core.domain.member.vo.MemberId
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.selectOne
import org.jooq.impl.DSL.table
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class MemberAuthorityRepository(
    private val dsl: DSLContext,
) : MemberAuthorityPersistencePort {
    override fun ensureAuthorityAssigned(
        memberId: MemberId,
        authorityName: String,
    ) {
        val authorityId = findAuthorityIdByName(authorityName)
        val memberIdField = field(name("member_id"), Long::class.java)
        val authorityIdField = field(name("authority_id"), Long::class.java)
        val deletedAtField = field(name("deleted_at"), LocalDateTime::class.java)

        val exists =
            dsl.fetchExists(
                dsl
                    .selectOne()
                    .from(table(name("member_authorities")))
                    .where(memberIdField.eq(memberId.value))
                    .and(authorityIdField.eq(authorityId))
                    .and(deletedAtField.isNull),
            )

        if (!exists) {
            dsl
                .insertInto(table(name("member_authorities")))
                .set(memberIdField, memberId.value)
                .set(authorityIdField, authorityId)
                .set(field(name("granted_at"), LocalDateTime::class.java), LocalDateTime.now(ZoneId.of(TIME_ZONE)))
                .execute()
        }
    }

    override fun revokeAuthority(
        memberId: MemberId,
        authorityName: String,
    ) {
        val authorityId = findAuthorityIdByName(authorityName)

        dsl
            .update(table(name("member_authorities")))
            .set(field(name("deleted_at"), LocalDateTime::class.java), LocalDateTime.now(ZoneId.of(TIME_ZONE)))
            .where(field(name("member_id"), Long::class.java).eq(memberId.value))
            .and(field(name("authority_id"), Long::class.java).eq(authorityId))
            .and(field(name("deleted_at"), LocalDateTime::class.java).isNull)
            .execute()
    }

    private fun findAuthorityIdByName(authorityName: String): Long {
        val authorityIdField = field(name("authority_id"), Long::class.java)

        return dsl
            .select(authorityIdField)
            .from(table(name("authorities")))
            .where(field(name("name"), String::class.java).eq(authorityName))
            .fetchOne(authorityIdField)
            ?: throw IllegalArgumentException("Authority not found: $authorityName")
    }

    companion object {
        private const val TIME_ZONE = "Asia/Seoul"
    }
}

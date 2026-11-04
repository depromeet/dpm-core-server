package core.persistence.member.repository

import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberMergePersistencePort
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_CREDENTIALS
import org.jooq.dsl.tables.references.MEMBER_OAUTH
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime

@Repository
class MemberMergeRepository(private val dsl: DSLContext) : MemberMergePersistencePort {
    override fun findTarget(sourceId: Long): core.domain.member.port.outbound.MemberMergeTarget? {
        val rows =
            dsl.fetch(
                "SELECT e.retained_member_id, e.created_at FROM member_merge_events e " +
                    "JOIN members m ON m.member_id = e.source_member_id " +
                    "WHERE e.source_member_id = ? AND m.status = 'WITHDRAWN' AND m.deleted_at IS NOT NULL",
                sourceId,
            )
        val row = rows.singleOrNull() ?: return null
        return core.domain.member.port.outbound.MemberMergeTarget(
            row.get("retained_member_id", Long::class.java)!!,
            row.get("created_at", Timestamp::class.java)!!.toInstant(),
        )
    }

    override fun transferRefreshTokens(
        sourceId: Long,
        retainedId: Long,
    ) {
        dsl.execute("UPDATE refresh_tokens SET member_id = ? WHERE member_id = ?", retainedId, sourceId)
    }

    override fun lockOAuths(memberIds: List<Long>): List<MemberOAuth> =
        dsl.selectFrom(MEMBER_OAUTH).where(MEMBER_OAUTH.MEMBER_ID.`in`(memberIds))
            .orderBy(MEMBER_OAUTH.MEMBER_OAUTH_ID.asc()).forUpdate().fetch().map {
                MemberOAuth(
                    MemberOAuthId(it.memberOauthId!!),
                    it.externalId!!,
                    OAuthProvider.valueOf(it.provider!!),
                    MemberId(it.memberId!!),
                    it.email,
                )
            }

    override fun hasPasswordCredential(memberIds: List<Long>): Boolean =
        dsl.select(MEMBER_CREDENTIALS.MEMBER_ID).from(MEMBER_CREDENTIALS)
            .where(MEMBER_CREDENTIALS.MEMBER_ID.`in`(memberIds)).forUpdate().fetch().isNotEmpty()

    override fun transferOAuths(
        sourceId: Long,
        retainedId: Long,
    ): Int =
        dsl.update(MEMBER_OAUTH).set(MEMBER_OAUTH.MEMBER_ID, retainedId)
            .where(MEMBER_OAUTH.MEMBER_ID.eq(sourceId)).execute()

    override fun softDeleteSource(memberId: Long) {
        val now = LocalDateTime.now()
        dsl.update(MEMBERS).set(MEMBERS.STATUS, "WITHDRAWN")
            .set(MEMBERS.DELETED_AT, now).set(MEMBERS.UPDATED_AT, now)
            .where(MEMBERS.MEMBER_ID.eq(memberId)).execute()
    }

    override fun recordMerge(
        actorId: Long,
        retainedId: Long,
        sourceId: Long,
    ) {
        dsl.execute(
            "INSERT INTO member_merge_events (actor_member_id, retained_member_id, source_member_id, created_at) " +
                "VALUES (?, ?, ?, ?)",
            actorId,
            retainedId,
            sourceId,
            Timestamp.from(Instant.now()),
        )
    }
}

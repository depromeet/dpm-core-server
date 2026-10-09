package core.persistence.member.repository

import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberProfile
import core.domain.member.port.outbound.MemberProfilePersistencePort
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_OAUTH
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class MemberProfileRepository(private val dsl: DSLContext) : MemberProfilePersistencePort {
    override fun findProfile(memberId: Long): MemberProfile? = read(memberId, lock = false)

    override fun lockProfile(memberId: Long): MemberProfile? = read(memberId, lock = true)

    private fun read(memberId: Long, lock: Boolean): MemberProfile? {
        val query = dsl.selectFrom(MEMBERS).where(MEMBERS.MEMBER_ID.eq(memberId))
        val record = (if (lock) query.forUpdate() else query).fetchOne() ?: return null
        return MemberProfile(
            memberId = requireNotNull(record.memberId),
            name = requireNotNull(record.name),
            // 과거의 공백/알 수 없는 파트도 보완 API를 통해 바로잡을 수 있어야 한다.
            part = record.part?.let { runCatching { MemberPart.valueOf(it) }.getOrNull() },
            status = MemberStatus.valueOf(requireNotNull(record.status)),
            deletedAt = record.deletedAt?.toInstant(ZoneOffset.UTC),
            completedAt = record.profileCompletedAt?.toInstant(ZoneOffset.UTC),
        )
    }

    override fun completeProfile(memberId: Long, name: String, part: MemberPart): Boolean {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return dsl.update(MEMBERS)
            .set(MEMBERS.NAME, name)
            .set(MEMBERS.PART, part.name)
            .set(MEMBERS.PROFILE_COMPLETED_AT, now)
            .set(MEMBERS.UPDATED_AT, now)
            .where(MEMBERS.MEMBER_ID.eq(memberId), MEMBERS.PROFILE_COMPLETED_AT.isNull)
            .execute() == 1
    }

    override fun hasAppleAccount(memberId: Long): Boolean =
        dsl.fetchExists(
            dsl.selectOne().from(MEMBER_OAUTH)
                .where(MEMBER_OAUTH.MEMBER_ID.eq(memberId), MEMBER_OAUTH.PROVIDER.eq(OAuthProvider.APPLE.name)),
        )
}

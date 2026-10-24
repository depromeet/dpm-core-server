package core.persistence.member.repository

import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberAccountPersistencePort
import core.domain.member.port.outbound.OAuthLinkRequest
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class MemberAccountRepository(private val dsl: DSLContext) : MemberAccountPersistencePort {
    override fun findOAuths(memberId: Long): List<MemberOAuth> =
        dsl.fetch(
            "SELECT member_oauth_id, member_id, provider, external_id, email FROM member_oauth " +
                "WHERE member_id = ? ORDER BY member_oauth_id",
            memberId,
        ).map {
            MemberOAuth(
                MemberOAuthId(it.get("member_oauth_id", Long::class.java)),
                it.get("external_id", String::class.java),
                OAuthProvider.valueOf(it.get("provider", String::class.java)),
                MemberId(memberId),
                it.get("email", String::class.java),
            )
        }

    override fun deleteOAuth(
        memberId: Long,
        oauthId: Long,
    ): Int = dsl.execute("DELETE FROM member_oauth WHERE member_id = ? AND member_oauth_id = ?", memberId, oauthId)

    override fun saveLinkRequest(request: OAuthLinkRequest) {
        dsl.execute(
            "INSERT INTO member_oauth_link_requests " +
                "(state_hash, member_id, provider, redirect_uri, code_verifier, expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
            request.stateHash,
            request.memberId,
            request.provider.name,
            request.redirectUri,
            request.codeVerifier,
            Timestamp.from(request.expiresAt),
        )
    }

    override fun lockLinkRequest(stateHash: String): OAuthLinkRequest? =
        dsl.fetchOne("SELECT * FROM member_oauth_link_requests WHERE state_hash = ? FOR UPDATE", stateHash)?.let {
            OAuthLinkRequest(
                stateHash,
                it.get("member_id", Long::class.java),
                OAuthProvider.valueOf(it.get("provider", String::class.java)),
                it.get("redirect_uri", String::class.java),
                it.get("code_verifier", String::class.java),
                it.get("expires_at", Timestamp::class.java).toInstant(),
            )
        }

    override fun deleteLinkRequest(stateHash: String) {
        dsl.execute("DELETE FROM member_oauth_link_requests WHERE state_hash = ?", stateHash)
    }

    override fun deleteLinkRequestsByMemberId(memberId: Long) {
        dsl.execute("DELETE FROM member_oauth_link_requests WHERE member_id = ?", memberId)
    }

    override fun deleteExpiredLinkRequests(now: Instant) {
        dsl.execute("DELETE FROM member_oauth_link_requests WHERE expires_at < ?", Timestamp.from(now))
    }
}

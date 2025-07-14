package com.server.dpmcore.member.memberAuthority.domain

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.member.member.domain.model.MemberId
import java.time.Instant

class MemberAuthority(
    val id: MemberAuthorityId? = null,
    val memberId: MemberId,
    val authorityId: AuthorityId,
    grantedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var grantedAt: Instant? = grantedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemberAuthority) return false

        return id == other.id &&
            memberId == other.memberId &&
            authorityId == other.authorityId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + memberId.hashCode()
        result = 31 * result + authorityId.hashCode()
        return result
    }

    override fun toString(): String =
        "MemberAuthority(id=$id, memberId=$memberId, authorityId=$authorityId, grantedAt=$grantedAt, " +
            "deletedAt=$deletedAt)"
}

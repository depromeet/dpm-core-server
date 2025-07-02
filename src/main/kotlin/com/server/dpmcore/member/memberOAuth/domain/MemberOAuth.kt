package com.server.dpmcore.member.memberOAuth.domain

import com.server.dpmcore.member.member.domain.model.MemberId

data class MemberOAuth(
    val id: MemberOAuthId? = null,
    val externalId: String,
    val provider: OAuthProvider,
    val memberId: MemberId,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemberOAuth) return false

        return id == other.id &&
            externalId == other.externalId &&
            provider == other.provider &&
            memberId == other.memberId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + externalId.hashCode()
        result = 31 * result + provider.hashCode()
        result = 31 * result + memberId.hashCode()
        return result
    }
}

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
        if (javaClass != other?.javaClass) return false

        other as MemberOAuth

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

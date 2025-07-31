package com.server.dpmcore.refreshToken.domain.model

import com.server.dpmcore.member.member.domain.model.MemberId

class RefreshToken(
    val memberId: MemberId,
    var token: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RefreshToken) return false
        return memberId == other.memberId
    }

    override fun hashCode(): Int = memberId.hashCode()

    companion object {
        fun create(
            memberId: MemberId,
            token: String,
        ): RefreshToken = RefreshToken(memberId, token)
    }

    fun rotate(token: String) {
        this.token = token
    }
}

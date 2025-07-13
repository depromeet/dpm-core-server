package com.server.dpmcore.refreshToken.domain.model

import com.server.dpmcore.member.member.domain.model.MemberId

class RefreshToken(
    val memberId: MemberId,
    var token: String
) {
    companion object {
        fun create(memberId: MemberId, token: String): RefreshToken {
            return RefreshToken(memberId, token)
        }
    }

    fun rotate(token: String) {
        this.token = token
    }
}

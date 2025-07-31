package com.server.dpmcore.refreshToken.domain.port.inbound

import com.server.dpmcore.member.member.domain.model.MemberId

interface RefreshTokenInvalidator {
    fun destroyRefreshToken(memberId: MemberId)
}

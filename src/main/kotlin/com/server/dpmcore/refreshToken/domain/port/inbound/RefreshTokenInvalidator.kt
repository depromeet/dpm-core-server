package com.server.dpmcore.refreshToken.domain.port.inbound

import com.server.dpmcore.member.member.domain.model.MemberId

interface RefreshTokenInvalidator {
    /**
     * 멤버 ID에 해당하는 멤버의 Refresh Token을 삭제함.
     *
     * @author LeeHanEum
     * @since 2025.07.17
     */
    fun destroyRefreshToken(memberId: MemberId)
}

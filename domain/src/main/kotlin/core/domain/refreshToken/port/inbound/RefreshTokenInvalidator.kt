package core.domain.refreshToken.port.inbound

import core.domain.member.vo.MemberId

interface RefreshTokenInvalidator {
    /**
     * 멤버 식별자에 해당하는 멤버의 Refresh Token을 삭제함.
     *
     * @author LeeHanEum
     * @since 2025.07.17
     */
    fun destroyRefreshToken(memberId: MemberId)
}

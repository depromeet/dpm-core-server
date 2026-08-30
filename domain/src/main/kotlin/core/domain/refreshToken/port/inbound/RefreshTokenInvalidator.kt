package core.domain.refreshToken.port.inbound

import core.domain.member.vo.MemberId

interface RefreshTokenInvalidator {
    /**
     * 해당 회원의 모든 기기 세션을 끊는다. 회원 탈퇴 · 하드 삭제용.
     *
     * @author LeeHanEum
     * @since 2025.07.17
     */
    fun destroyRefreshToken(memberId: MemberId)

    /** 토큰 해시 하나에 해당하는 기기 세션만 끊는다. 일반 로그아웃용. */
    fun destroyByTokenHash(tokenHash: String)

    /** 지정한 토큰의 기기만 남기고 나머지를 끊는다. "다른 기기에서 모두 로그아웃"용. */
    fun destroyOtherDevices(
        memberId: MemberId,
        keepTokenHash: String,
    )
}

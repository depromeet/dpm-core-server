package core.domain.member.port.inbound

import core.domain.security.oauth.dto.LoginResult
import core.domain.security.oauth.dto.OAuthAttributes

interface HandleMemberLoginUseCase {
    /**
     * 멤버 로그인 처리 후 프론트엔드가 후속 분기를 이어서 처리할 수 있도록 결과를 반환함.
     *
     * @param deviceId 기기 식별자. null 이면 기기 구분 없이 새 세션 행만 추가된다.
     *
     * @author leehaneum
     * @since 2025.08.15
     */
    fun handleLoginSuccess(
        authAttributes: OAuthAttributes,
        deviceId: String? = null,
    ): LoginResult
}

package core.application.member.application.service.auth

import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.refreshToken.application.service.RefreshTokenIssueService
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.vo.MemberId
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KakaoLoginTokenSaveService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenInjector: JwtTokenInjector,
    private val refreshTokenIssueService: RefreshTokenIssueService,
    private val deviceIdResolver: DeviceIdResolver,
) {
    private val logger = KotlinLogging.logger { }

    /**
     * 클라이언트가 들고 있던 리프레시 토큰의 소유자를 확인한 뒤,
     * 해당 기기에 새 세션 체인을 발급하고 쿠키로 내려준다.
     *
     * 전달받은 토큰을 그대로 저장하지 않고 새로 발급하는 이유는,
     * 회전 이력을 서버가 온전히 통제하기 위해서다.
     */
    @Transactional
    fun save(
        refreshToken: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            logger.warn { "Rejected Kakao token save request: invalid refresh token" }
            throw TokenInvalidException()
        }

        val memberId = MemberId(jwtTokenProvider.getMemberId(refreshToken))
        val deviceId = deviceIdResolver.resolve(request, response)
        val issued = refreshTokenIssueService.issueForLogin(memberId, deviceId)

        val accessToken = jwtTokenProvider.generateAccessToken(memberId.toString())
        jwtTokenInjector.injectAccessToken(accessToken, response)
        jwtTokenInjector.injectRefreshToken(issued, response)
    }
}

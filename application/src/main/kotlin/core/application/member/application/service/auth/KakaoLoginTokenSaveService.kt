package core.application.member.application.service.auth

import core.application.refreshToken.application.service.RefreshTokenService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service

@Service
class KakaoLoginTokenSaveService(
    private val refreshTokens: RefreshTokenService,
) {
    /** Exchange only the backend refresh token in the body, including sessions transferred by a merge. */
    fun save(
        refreshToken: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val presented =
            object : HttpServletRequestWrapper(request) {
                override fun getHeader(name: String): String? =
                    if (name.equals(
                            "Authorization",
                            ignoreCase = true,
                        )
                    ) {
                        "Bearer $refreshToken"
                    } else {
                        super.getHeader(name)
                    }

                override fun getCookies(): Array<Cookie> = emptyArray()
            }
        refreshTokens.reissue(presented, response)
    }
}

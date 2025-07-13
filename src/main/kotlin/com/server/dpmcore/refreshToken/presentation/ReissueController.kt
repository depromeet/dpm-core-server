package com.server.dpmcore.refreshToken.presentation

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.refreshToken.application.RefreshTokenService
import com.server.dpmcore.refreshToken.presentation.response.TokenResponse
import com.server.dpmcore.security.properties.TokenProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ReissueController(
    private val refreshTokenService: RefreshTokenService,
    private val tokenProperties: TokenProperties
) : ReissueApi {

    @PostMapping("/v1/reissue")
    override fun reissue(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): CustomResponse<TokenResponse> {
        val tokenResult = refreshTokenService.reissueBasedOnRefreshToken(request, response)
        return CustomResponse.ok(TokenResponse.of(tokenResult, tokenProperties))
    }

}

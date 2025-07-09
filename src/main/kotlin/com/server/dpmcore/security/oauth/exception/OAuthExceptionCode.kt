package com.server.dpmcore.security.oauth.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class OAuthExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String
) : ExceptionCode {
    AUTHENTICATION_FAILED(HttpStatus.BAD_REQUEST, "A400", "소셜 로그인에 실패했습니다"),
    ;

    override fun getStatus(): HttpStatus = status
    override fun getCode(): String = code
    override fun getMessage(): String = message
}

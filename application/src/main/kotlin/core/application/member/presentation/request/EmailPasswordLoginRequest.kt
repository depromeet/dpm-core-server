package core.application.member.presentation.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * 이메일 비밀번호 로그인 요청 DTO입니다.
 */
data class EmailPasswordLoginRequest(
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String,
)

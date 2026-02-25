package core.application.member.presentation.request

import jakarta.validation.constraints.NotBlank

/**
 * 이메일 비밀번호 설정/변경 요청 DTO입니다.
 */
data class SetPasswordRequest(
    @field:NotBlank(message = "비밀번호는 필수입니다")
    val newPassword: String,
    @field:NotBlank(message = "비밀번호 확인은 필수입니다")
    val confirmPassword: String,
    // 기존 비밀번호가 있는 경우에만 필요
    val oldPassword: String? = null,
)

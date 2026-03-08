package core.application.member.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class WhiteListCheckRequest(
    @field:Valid
    @field:NotEmpty
    @field:Schema(
        description = "화이트리스트 체크 대상 멤버 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val members: List<MemberWhiteListCheckRequest>,
) {
    data class MemberWhiteListCheckRequest(
        @field:NotBlank
        @field:Email
        @field:Schema(
            description = "이메일",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val email: String,
    )
}

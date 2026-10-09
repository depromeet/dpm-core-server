package core.application.member.presentation.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class MemberProfileUpdateRequest(
    @field:NotBlank
    @field:Schema(description = "이름. 앞뒤 공백 제거 후 완성형 한글과 단어 사이 공백만 허용하며 최대 255자")
    val name: String,
    @field:NotBlank
    @field:Schema(description = "파트", allowableValues = ["WEB", "SERVER", "DESIGN", "IOS", "ANDROID"])
    val part: String,
) {
    @JsonAnySetter
    fun rejectUnknownField(
        field: String,
        value: Any?,
    ): Nothing = throw IllegalArgumentException("지원하지 않는 프로필 수정 필드입니다: $field")
}

package core.application.member.presentation.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class MemberApprovalRequest(
    @field:NotEmpty
    @field:JsonDeserialize(contentUsing = MemberManagementLongDeserializer::class)
    @field:Schema(description = "승인할 멤버 ID 목록. 단건도 배열로 전달. 빈 목록, 중복, null, 0 이하 ID는 허용하지 않음")
    val members: List<Long?>,
) {
    @JsonAnySetter
    fun rejectUnknownField(
        field: String,
        value: Any?,
    ): Nothing = throw IllegalArgumentException("지원하지 않는 가입 승인 필드입니다: $field")
}

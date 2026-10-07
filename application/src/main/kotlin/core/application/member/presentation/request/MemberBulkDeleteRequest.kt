package core.application.member.presentation.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty

data class MemberBulkDeleteRequest(
    @field:NotEmpty
    @field:JsonDeserialize(contentUsing = MemberManagementLongDeserializer::class)
    @field:Schema(description = "영구 삭제할 멤버 ID 목록. 중복, null, 0 이하 ID는 허용하지 않음", example = "[1,2]")
    val memberIds: List<Long?>,
) {
    @get:JsonIgnore
    @get:Schema(hidden = true)
    @get:AssertTrue(message = "중복되지 않은 양의 멤버 ID를 입력해주세요")
    val isValidMemberIds: Boolean
        get() = memberIds.all { it != null && it > 0 } && memberIds.distinct().size == memberIds.size

    @JsonAnySetter
    fun rejectUnknownField(
        field: String,
        value: Any?,
    ): Nothing = throw IllegalArgumentException("지원하지 않는 멤버 삭제 필드입니다: $field")
}

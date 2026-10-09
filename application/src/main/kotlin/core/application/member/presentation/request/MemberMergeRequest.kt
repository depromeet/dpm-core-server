package core.application.member.presentation.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class MemberMergeRequest(
    @field:NotNull
    @field:Positive
    @field:JsonDeserialize(using = MemberManagementLongDeserializer::class)
    @field:Schema(description = "통합 후 유지하고 승인할 미승인 멤버 ID", example = "1")
    val retainedMemberId: Long?,
    @field:NotNull
    @field:Positive
    @field:JsonDeserialize(using = MemberManagementLongDeserializer::class)
    @field:Schema(description = "로그인 수단을 옮긴 뒤 정리할 미승인 멤버 ID. 유지할 ID와 달라야 함", example = "2")
    val sourceMemberId: Long?,
) {
    @get:JsonIgnore
    @get:Schema(hidden = true)
    @get:AssertTrue(message = "서로 다른 멤버를 선택해주세요")
    val isDistinct: Boolean get() = retainedMemberId != sourceMemberId

    @JsonAnySetter
    fun rejectUnknownField(
        field: String,
        value: Any?,
    ): Nothing = throw IllegalArgumentException("지원하지 않는 계정 통합 필드입니다: $field")
}

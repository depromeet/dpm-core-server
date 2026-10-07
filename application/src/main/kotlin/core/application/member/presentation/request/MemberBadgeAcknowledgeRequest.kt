package core.application.member.presentation.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class MemberBadgeAcknowledgeRequest(
    @field:NotNull
    @field:Positive
    @field:JsonDeserialize(using = MemberManagementLongDeserializer::class)
    @field:Schema(description = "NEW 조회 응답의 기수 ID. 현재 관리 기수와 다르면 다시 조회", example = "19")
    val cohortId: Long?,
    @field:NotNull
    @field:Min(0)
    @field:JsonDeserialize(using = MemberManagementLongDeserializer::class)
    @field:Schema(description = "확인한 카드의 조회 시점 version. 서버 최신 버전보다 큰 값은 거절", example = "3")
    val version: Long?,
) {
    @JsonAnySetter
    fun rejectUnknownField(
        field: String,
        value: Any?,
    ): Nothing = throw IllegalArgumentException("지원하지 않는 NEW 확인 필드입니다: $field")
}

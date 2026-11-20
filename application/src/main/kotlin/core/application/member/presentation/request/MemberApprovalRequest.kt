package core.application.member.presentation.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

data class MemberApprovalRequest(
    @field:NotEmpty
    @field:JsonDeserialize(contentUsing = MemberManagementLongDeserializer::class)
    @field:Schema(description = "승인할 멤버 ID 목록. 단건도 배열로 전달. 빈 목록, 중복, null, 0 이하 ID는 허용하지 않음")
    val members: List<Long?>,
    @field:Schema(
        description =
            "현재 기수에서 맡을 멤버 타입. 생략/null은 DEEPER. ORGANIZER·CORE는 update:authorization 권한 필요. " +
                "새로 현재 기수에 들어가는 멤버에게만 적용",
        allowableValues = ["DEEPER", "ORGANIZER", "CORE"],
        nullable = true,
    )
    @field:Pattern(regexp = "DEEPER|ORGANIZER|CORE")
    val memberType: String? = null,
    @field:Schema(
        description = "현재 기수 팀 ID. 생략/null/0은 미배정. 목록의 팀 번호와 구별. 새로 현재 기수에 들어가는 멤버에게만 적용",
        nullable = true,
    )
    @field:Min(0)
    @field:JsonDeserialize(using = MemberManagementLongDeserializer::class)
    val teamId: Long? = null,
) {
    @JsonAnySetter
    fun rejectUnknownField(
        field: String,
        value: Any?,
    ): Nothing = throw IllegalArgumentException("지원하지 않는 가입 승인 필드입니다: $field")
}

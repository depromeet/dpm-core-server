package core.application.member.presentation.request

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

data class MemberManagementUpdateRequest(
    @field:Schema(description = "변경할 파트. UNASSIGNED는 미배정, 생략/null은 유지")
    @field:Pattern(regexp = "WEB|SERVER|DESIGN|IOS|ANDROID|UNASSIGNED")
    val part: String? = null,
    @field:Schema(description = "현재 기수 팀 ID. 0은 미배정, 생략/null은 유지. 목록의 팀 번호와 구별")
    @field:Min(0)
    @field:JsonDeserialize(using = MemberManagementLongDeserializer::class)
    val teamId: Long? = null,
    @field:Schema(description = "변경할 멤버 타입. UNASSIGNED는 미배정, 생략/null은 유지. update:authorization 권한 필요")
    @field:Pattern(regexp = "DEEPER|ORGANIZER|CORE|UNASSIGNED")
    val memberType: String? = null,
    @field:Schema(description = "변경할 활동 상태. 수료 위험은 자동 계산하므로 직접 수정하지 않음")
    @field:Pattern(regexp = "ACTIVE|INACTIVE")
    val status: String? = null,
) {
    @JsonAnySetter
    fun rejectUnknownField(
        field: String,
        value: Any?,
    ): Nothing = throw IllegalArgumentException("지원하지 않는 멤버 수정 필드입니다: $field")
}

data class MemberManagementBulkUpdateRequest(
    @field:Schema(description = "수정할 멤버 ID 목록. 빈 목록, 중복 ID, null, 0 이하 ID는 허용하지 않음")
    @field:NotEmpty
    @field:JsonDeserialize(contentUsing = MemberManagementLongDeserializer::class)
    val memberIds: List<Long?>,
    @field:Schema(description = "한 컬럼만 지정하며 선택한 모든 멤버에게 같은 값을 적용")
    @field:Valid
    val changes: MemberManagementUpdateRequest,
) {
    @JsonAnySetter
    fun rejectUnknownField(
        field: String,
        value: Any?,
    ): Nothing = throw IllegalArgumentException("지원하지 않는 일괄 수정 필드입니다: $field")
}

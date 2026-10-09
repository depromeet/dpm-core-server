package core.application.member.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

data class MemberProfileResponse(
    val name: String,
    val part: String?,
    @field:Schema(description = "최초 프로필 입력이 필요한지 여부. 완료 후 관리자 파트 미배정으로 다시 활성화되지 않음")
    val profileCompletionRequired: Boolean,
)

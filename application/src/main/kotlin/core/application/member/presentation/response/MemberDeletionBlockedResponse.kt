package core.application.member.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

data class MemberDeletionBlockedResponse(
    @field:Schema(description = "요청 대상 중 공유 기록 때문에 삭제할 수 없는 회원 ID 전체. 중복 없이 오름차순", example = "[12,34]")
    val blockedMemberIds: List<Long>,
)

package core.application.member.presentation.request

import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class WhiteListCheckRequest(
    @field:NotEmpty
    @field:Schema(
        description = "화이트리스트 체크 대상 멤버 목록",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val members: List<Long>,
) {
    fun toMemberIds(): List<MemberId> = members.map(::MemberId)
}

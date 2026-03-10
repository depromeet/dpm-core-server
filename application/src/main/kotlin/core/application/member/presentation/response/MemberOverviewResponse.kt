package core.application.member.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

data class MemberOverviewResponse(
    @field:Schema(description = "멤버 목록")
    val members: List<MemberSummary>,
) {
    data class MemberSummary(
        @field:Schema(description = "멤버 ID", example = "1")
        val memberId: Long,
        @field:Schema(description = "기수 ID", example = "17")
        val cohortId: Long?,
        @field:Schema(description = "멤버 이름", example = "홍길동")
        val name: String,
        @field:Schema(description = "팀 이름", example = "1팀")
        val teamName: String,
        @field:Schema(description = "멤버 상태", example = "PENDING")
        val status: String,
        @field:Schema(description = "멤버 직무", example = "SERVER")
        val part: String,
    )

    companion object {
        fun of(members: List<MemberSummary>): MemberOverviewResponse = MemberOverviewResponse(members)
    }
}

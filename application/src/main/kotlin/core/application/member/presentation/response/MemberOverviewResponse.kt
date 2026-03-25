package core.application.member.presentation.response

import core.domain.team.vo.TeamNumber
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
        @field:Schema(description = "팀 이름", example = "1")
        val teamNumber: TeamNumber,
        @field:Schema(description = "운영진 여부", example = "false")
        val isAdmin: Boolean,
        @field:Schema(description = "멤버 상태", example = "PENDING")
        val status: String,
        @field:Schema(description = "멤버 직무", example = "SERVER")
        val part: String,
    )

    companion object {
        fun of(members: List<MemberSummary>): MemberOverviewResponse = MemberOverviewResponse(members)
    }
}

package core.application.announcement.presentation.response

import core.domain.announcement.enums.SubmitStatus
import core.domain.member.enums.MemberPart
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamId

data class AssignmentStatusMemberListItemResponse(
    val memberId: MemberId,
    val name: String,
    val teamId: TeamId,
    val part: MemberPart?,
    val submitStatus: SubmitStatus,
    val score: Int?,
) {
    companion object {
        fun of(
            memberId: MemberId,
            name: String,
            teamId: TeamId,
            part: MemberPart?,
            submitStatus: SubmitStatus,
            score: Int?,
        ): AssignmentStatusMemberListItemResponse =
            AssignmentStatusMemberListItemResponse(
                memberId = memberId,
                name = name,
                teamId = teamId,
                part = part,
                submitStatus = submitStatus,
                score = score,
            )
    }
}

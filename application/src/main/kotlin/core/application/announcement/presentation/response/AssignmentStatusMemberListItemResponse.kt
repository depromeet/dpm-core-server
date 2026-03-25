package core.application.announcement.presentation.response

import core.domain.announcement.enums.SubmitStatus
import core.domain.member.enums.MemberPart
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamNumber

data class AssignmentStatusMemberListItemResponse(
    val memberId: MemberId,
    val name: String,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val part: MemberPart?,
    val submitStatus: SubmitStatus,
    val score: Int?,
) {
    companion object {
        fun of(
            memberId: MemberId,
            name: String,
            teamNumber: TeamNumber,
            isAdmin: Boolean,
            part: MemberPart?,
            submitStatus: SubmitStatus,
            score: Int?,
        ): AssignmentStatusMemberListItemResponse =
            AssignmentStatusMemberListItemResponse(
                memberId = memberId,
                name = name,
                teamNumber = teamNumber,
                isAdmin = isAdmin,
                part = part,
                submitStatus = submitStatus,
                score = score,
            )
    }
}

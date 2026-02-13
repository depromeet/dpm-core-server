package core.domain.announcement.port.inbound.command

import core.domain.member.vo.MemberId

data class UpdateSubmitStatusMemberDetailCommand(
    val memberId: MemberId,
    val score: Int?,
) {
    companion object {
        fun of(
            memberId: MemberId,
            score: Int?,
        ): UpdateSubmitStatusMemberDetailCommand {
            return UpdateSubmitStatusMemberDetailCommand(
                memberId = memberId,
                score = score,
            )
        }
    }
}

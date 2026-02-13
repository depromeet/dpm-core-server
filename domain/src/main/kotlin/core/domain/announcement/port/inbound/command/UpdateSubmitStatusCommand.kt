package core.domain.announcement.port.inbound.command

import core.domain.announcement.enums.SubmitStatus
import core.domain.announcement.vo.AnnouncementId

data class UpdateSubmitStatusCommand(
    val announcementId: AnnouncementId,
    val assignmentSubmitStatus: SubmitStatus,
    val members: List<UpdateSubmitStatusMemberDetailCommand>,
) {
    companion object {
        fun of(
            announcementId: AnnouncementId,
            assignmentSubmitStatus: SubmitStatus,
            members: List<UpdateSubmitStatusMemberDetailCommand>,
        ): UpdateSubmitStatusCommand {
            return UpdateSubmitStatusCommand(
                announcementId = announcementId,
                assignmentSubmitStatus = assignmentSubmitStatus,
                members = members,
            )
        }
    }
}

package core.application.announcement.presentation.request

import core.domain.announcement.enums.SubmitStatus

data class UpdateSubmitStatusRequest(
    val assignmentSubmitStatus: SubmitStatus,
    val members: List<UpdateSubmitStatusMemberDetailRequest>,
)

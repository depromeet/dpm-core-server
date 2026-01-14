package core.application.bill.presentation.response

import core.domain.bill.port.outbound.query.BillMemberIsInvitationSubmittedQueryModel

data class BillMemberSubmittedListResponse(
    val members: List<BillMemberIsInvitationSubmittedQueryModel>,
)

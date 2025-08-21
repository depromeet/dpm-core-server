package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.bill.bill.domain.port.inbound.query.BillMemberIsInvitationSubmittedQueryModel

data class BillMemberSubmittedListResponse(
    val members: List<BillMemberIsInvitationSubmittedQueryModel>,
)

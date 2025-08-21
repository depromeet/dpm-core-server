package com.server.dpmcore.bill.bill.domain.port.inbound.query

data class BillMemberIsInvitationSubmittedQueryModel(
    val name: String,
    val teamNumber: Int,
    val authority: String,
    val part: String?,
    val isInvitationSubmitted: Boolean,
)

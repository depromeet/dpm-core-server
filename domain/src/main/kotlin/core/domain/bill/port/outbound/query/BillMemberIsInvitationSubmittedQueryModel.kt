package core.domain.bill.port.outbound.query

data class BillMemberIsInvitationSubmittedQueryModel(
    val name: String,
    val teamNumber: Int,
    val authority: String,
    val part: String?,
    val isInvitationSubmitted: Boolean,
)

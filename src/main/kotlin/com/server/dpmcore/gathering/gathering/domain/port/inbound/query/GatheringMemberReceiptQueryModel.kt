package com.server.dpmcore.gathering.gathering.domain.port.inbound.query

data class GatheringMemberReceiptQueryModel(
    val memberName: String,
    val memberAuthority: String,
    val memberSplitAmount: Int?,
) {
    companion object {
        fun of(
            memberName: String,
            memberAuthority: String,
            memberSplitAmount: Int?,
        ): GatheringMemberReceiptQueryModel =
            GatheringMemberReceiptQueryModel(
                memberName = memberName,
                memberAuthority = memberAuthority,
                memberSplitAmount = memberSplitAmount,
            )
    }
}

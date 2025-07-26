package com.server.dpmcore.bill.bill.presentation.dto.response

data class BillSummaryListByMemberResponse(
    val members: List<BillSummaryByMemberResponse>,
) {
    data class BillSummaryByMemberResponse(
        val memberName: String,
        val memberAuthority: String,
        val memberSplitAmount: Int,
    ) {
        companion object {
            fun of(
                memberName: String,
                memberAuthority: String,
                memberSplitAmount: Int,
            ): BillSummaryByMemberResponse =
                BillSummaryByMemberResponse(
                    memberName = memberName,
                    memberAuthority = memberAuthority,
                    memberSplitAmount = memberSplitAmount,
                )
        }
    }

    companion object {
        fun from(members: List<BillSummaryByMemberResponse>): BillSummaryListByMemberResponse =
            BillSummaryListByMemberResponse(members)
    }
}

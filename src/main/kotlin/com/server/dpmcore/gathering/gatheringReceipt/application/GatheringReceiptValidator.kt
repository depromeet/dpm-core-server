package com.server.dpmcore.gathering.gatheringReceipt.application

import com.server.dpmcore.gathering.gatheringReceipt.application.exception.MemberCountMustOverOneException
import com.server.dpmcore.gathering.gatheringReceipt.application.exception.ReceiptAlreadySplitException
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import org.springframework.stereotype.Component

@Component
class GatheringReceiptValidator {
    fun checkIsExistsSplitAmount(receipt: GatheringReceipt) {
        if (receipt.isExistsSplitAmount()) throw ReceiptAlreadySplitException()
    }

    fun checkJoinMemberCountMoreThenOne(
        receipt: GatheringReceipt,
        joinMemberCount: Int,
    ) {
        if (receipt.validateJoinMemberCount(joinMemberCount)) throw MemberCountMustOverOneException()
    }
}

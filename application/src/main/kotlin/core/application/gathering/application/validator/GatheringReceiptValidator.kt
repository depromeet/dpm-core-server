package core.application.gathering.application.validator

import core.application.gathering.application.exception.receipt.MemberCountMustOverOneException
import core.application.gathering.application.exception.receipt.ReceiptAlreadySplitException
import core.domain.gathering.aggregate.GatheringReceipt
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

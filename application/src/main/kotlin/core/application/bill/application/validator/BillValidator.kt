package core.application.bill.application.validator

import core.application.bill.application.exception.BillAlreadyCompletedException
import core.domain.bill.aggregate.Bill
import org.springframework.stereotype.Component

@Component
class BillValidator {
    fun checkIsBillClosable(bill: Bill) {
        if (bill.isParticipationClosable()) throw BillAlreadyCompletedException()
    }

    fun checkIsBillCompleted(bill: Bill) {
        if (bill.isCompleted()) throw BillAlreadyCompletedException()
    }
}

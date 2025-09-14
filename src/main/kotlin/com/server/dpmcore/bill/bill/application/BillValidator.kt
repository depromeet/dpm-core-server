package com.server.dpmcore.bill.bill.application

import com.server.dpmcore.bill.bill.application.exception.BillAlreadyCompletedException
import com.server.dpmcore.bill.bill.domain.model.Bill
import org.springframework.stereotype.Component

@Component
class BillValidator {
    fun checkIsBillClosable(bill: Bill) {
        if (bill.checkParticipationClosable()) throw BillAlreadyCompletedException()
    }

    fun checkIsBillCompleted(bill: Bill) {
        if (bill.isCompleted()) throw BillAlreadyCompletedException()
    }
}

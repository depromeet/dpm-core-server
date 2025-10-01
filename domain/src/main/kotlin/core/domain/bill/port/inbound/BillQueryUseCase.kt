package core.domain.bill.port.inbound

import core.domain.bill.aggregate.Bill
import core.domain.bill.vo.BillId

interface BillQueryUseCase {
    fun getById(billId: BillId): Bill
}

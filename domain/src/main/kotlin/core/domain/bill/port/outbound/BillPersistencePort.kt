package core.domain.bill.port.outbound

import core.domain.bill.aggregate.Bill
import core.domain.bill.vo.BillId

interface BillPersistencePort {
    fun save(bill: Bill): BillId

    fun findById(billId: BillId): Bill?

    fun findAllBills(): List<Bill>
}

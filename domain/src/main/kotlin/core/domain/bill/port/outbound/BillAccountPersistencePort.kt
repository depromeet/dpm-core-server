package core.domain.bill.port.outbound

import core.domain.bill.aggregate.BillAccount
import core.domain.bill.vo.BillAccountId

interface BillAccountPersistencePort {
    fun findById(billAccountId: BillAccountId): BillAccount?
}

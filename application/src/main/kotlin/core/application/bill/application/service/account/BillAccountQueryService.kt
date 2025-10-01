package core.application.bill.application.service.account

import core.application.bill.application.exception.account.BillAccountNotFoundException
import core.domain.bill.aggregate.BillAccount
import core.domain.bill.port.outbound.BillAccountPersistencePort
import core.domain.bill.vo.BillAccountId
import org.springframework.stereotype.Service

@Service
class BillAccountQueryService(
    private val billAccountPersistencePort: BillAccountPersistencePort,
) {
    fun findBy(billAccountId: BillAccountId): BillAccount =
        billAccountPersistencePort.findById(
            billAccountId,
        ) ?: throw BillAccountNotFoundException()
}

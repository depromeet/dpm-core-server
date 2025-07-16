package com.server.dpmcore.bill.billAccount.application

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.port.BillAccountRepositoryPort
import org.springframework.stereotype.Service

@Service
class BillAccountReadService(
    private val billAccountRepositoryPort: BillAccountRepositoryPort,
) {
    fun findBy(billAccount: BillAccount): BillAccount {
        return billAccountRepositoryPort.findById(
            billAccount.id?.value
                ?: throw IllegalArgumentException("BillAccount ID는 null일 수 없습니다."),
        )
    }
}

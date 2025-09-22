package core.persistence.bill.repository.account

import core.domain.bill.aggregate.BillAccount
import core.domain.bill.port.outbound.BillAccountPersistencePort
import core.domain.bill.vo.BillAccountId
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
class BillAccountRepository(
    private val billAccountJpaRepository: BillAccountJpaRepository,
) : BillAccountPersistencePort {
    override fun findById(billAccountId: BillAccountId): BillAccount? =
        billAccountJpaRepository.findById(billAccountId.value).getOrNull()?.toDomain()
}

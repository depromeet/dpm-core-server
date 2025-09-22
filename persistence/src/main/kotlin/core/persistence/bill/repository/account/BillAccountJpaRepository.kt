package core.persistence.bill.repository.account

import com.server.dpmcore.bill.billAccount.infrastructure.entity.BillAccountEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BillAccountJpaRepository : JpaRepository<BillAccountEntity, Long>

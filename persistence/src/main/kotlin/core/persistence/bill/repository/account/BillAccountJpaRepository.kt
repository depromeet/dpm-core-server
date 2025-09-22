package core.persistence.bill.repository.account

import core.entity.bill.BillAccountEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BillAccountJpaRepository : JpaRepository<BillAccountEntity, Long>

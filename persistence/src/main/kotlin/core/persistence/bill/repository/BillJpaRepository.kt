package core.persistence.bill.repository

import core.persistence.bill.entity.BillEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BillJpaRepository : JpaRepository<BillEntity, Long>

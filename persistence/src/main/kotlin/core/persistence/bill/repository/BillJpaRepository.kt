package core.persistence.bill.repository

import core.entity.bill.BillEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BillJpaRepository : JpaRepository<BillEntity, Long>

package com.server.dpmcore.bill.bill.infrastructure.repository

import com.server.dpmcore.bill.bill.infrastructure.entity.BillEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BillJpaRepository : JpaRepository<BillEntity, Long>

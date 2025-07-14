package com.server.dpmcore.gathering.gatheringReceipt.infrastructure.repository

import com.server.dpmcore.gathering.gatheringReceipt.infrastructure.entity.ReceiptEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReceiptJpaRepository : JpaRepository<ReceiptEntity, String>

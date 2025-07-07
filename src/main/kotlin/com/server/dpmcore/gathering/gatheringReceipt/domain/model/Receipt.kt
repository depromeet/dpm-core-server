package com.server.dpmcore.gathering.gatheringReceipt.domain.model

import com.server.dpmcore.gathering.gathering.domain.model.Gathering
import com.server.dpmcore.gathering.gatheringReceiptPhoto.domain.model.ReceiptPhoto
import java.time.Instant

/**
 * Receipt는 회식 차수(Gathering) 내 실제 지출을 나타내는 영수증 정보입니다.
 *
 * - 각 Receipt는 이미지(영수증 사진), 전체 금액, 1인당 분할 금액, 확정 여부 등을 가집니다.
 * - 하나의 Gathering에 여러 개의 Receipt가 연결될 수 있습니다.
 * - 영수증 확정 여부(isCompleted)는 수정 가능 여부를 제어합니다.
 */
class Receipt(
    val id: ReceiptId? = null,
    val splitAmount: Int,
    val totalAmount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val receiptPhotos: MutableList<ReceiptPhoto> = mutableListOf(),
    val gathering: Gathering,
) {

    fun isDeleted(): Boolean = deletedAt != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Receipt) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}


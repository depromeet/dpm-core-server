package com.server.dpmcore.gathering.gatheringReceiptPhoto.domain.model

import com.server.dpmcore.gathering.gatheringReceipt.domain.model.Receipt
import java.time.Instant

/**
 * ReceiptPhoto는 하나의 영수증(Receipt)에 첨부된 사진(URL)을 나타냅니다.
 *
 * - 각 Receipt에는 여러 장의 이미지가 첨부될 수 있습니다 (250706 정책상 최대 3장).
 * - 정책에 따라 1차에는 제외될 수 있으나, 도메인 레벨에서는 확장성 있게 관리합니다.
 * - 삭제는 소프트 딜리트 처리됩니다.
 */
class ReceiptPhoto(
    val id: ReceiptPhotoId? = null,
    val url: String,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val receipt: Receipt,
) {
    fun isDeleted(): Boolean = deletedAt != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReceiptPhoto) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

package com.server.dpmcore.gathering.gathering.domain.model

import com.server.dpmcore.bill.bill.domain.model.Bill
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.member.member.domain.model.MemberId
import java.time.Instant

/**
 * 회식 차수(Gathering)를 표현하는 도메인 모델입니다.
 *
 * 하나의 Bill(정산 단위)에 여러 Gathering(1차, 2차, ...)가 속할 수 있습니다.
 *
 * - 최대 5개까지 차수를 생성할 수 있습니다.
 * - 각 차수에는 제목, 설명, 개최일, 회식 차수 번호 등이 포함됩니다.
 * - 운영진은 차수별로 참석자를 확정하고 금액을 관리합니다.
 *
 * 주의:
 * - 삭제는 소프트 딜리트 처리됩니다.
 */
class Gathering(
    val id: GatheringId? = null,
    val title: String,
    val description: String? = null,
    val heldAt: Instant,
    val category: GatheringCategory? = null,
    val hostUserId: MemberId? = null,
    val roundNumber: Int,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val bill: Bill? = null,
    val gatheringMembers: MutableList<GatheringMember>? = mutableListOf(),
) {
    fun isDeleted(): Boolean = deletedAt != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Gathering) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

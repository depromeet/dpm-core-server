package com.server.dpmcore.gathering.gathering.domain.model

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.gathering.gathering.domain.port.inbound.command.GatheringCreateCommand
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.member.member.domain.model.MemberId
import java.time.Instant
import java.time.ZoneId

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
    val category: GatheringCategory = GatheringCategory.GATHERING,
    val hostUserId: MemberId,
    val roundNumber: Int,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
    billId: BillId? = null,
    var gatheringMembers: MutableList<GatheringMember> = mutableListOf(),
    var gatheringReceipt: GatheringReceipt? = null,
) {
    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    var billId: BillId? = billId
        private set

    fun isDeleted(): Boolean = deletedAt != null

    //    TODO : 회식 멤버가 null일 수 있음
    fun getGatheringJoinMemberCount() =
        gatheringMembers.count { gatheringMember ->
            (gatheringMember.isJoined == true) && gatheringMember.deletedAt == null
        }

    fun getBillViewCount() =
        gatheringMembers.count { gatheringMember ->
            gatheringMember.isViewed
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Gathering) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String =
        "Gathering(id=$id, title='$title', description=$description, heldAt=$heldAt, category=$category, " +
            "hostUserId=$hostUserId, roundNumber=$roundNumber, createdAt=$createdAt, updatedAt=$updatedAt, " +
            "deletedAt=$deletedAt, bill=$billId, gatheringMembers=$gatheringMembers)"

    companion object {
        fun create(command: GatheringCreateCommand): Gathering =
            Gathering(
                title = command.title,
                description = command.description,
                heldAt = command.heldAt.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                hostUserId = command.hostUserId,
                roundNumber = command.roundNumber,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
    }
}

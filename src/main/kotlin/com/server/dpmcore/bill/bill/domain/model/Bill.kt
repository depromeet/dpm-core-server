package com.server.dpmcore.bill.bill.domain.model

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.exception.BillException
import com.server.dpmcore.gathering.exception.GatheringReceiptException
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gatheringMember.domain.model.GatheringMember
import com.server.dpmcore.gathering.gatheringReceipt.domain.model.GatheringReceipt
import com.server.dpmcore.member.member.domain.model.MemberId
import java.time.Instant

/**
 * 정산(Bill)을 표현하는 도메인 모델입니다.
 *
 * 이 모델은 하나의 회식 정산 단위를 나타내며, 여러 차수(1차, 2차 등)의 회식이 묶일 수 있습니다.
 * 1차 MVP에서는 운영진에 의해 생성되며, 멤버들의 참석 여부를 바탕으로 확정됩니다.
 *
 * - 정산 확정 이후에는 수정이 불가능하며, 삭제 후 재생성이 필요합니다.
 * - 회식 차수(Gathering)는 최대 5개까지 추가 가능하며, 각각의 금액, 설명 등을 가집니다.
 * - 정산 대상자는 각 차수에 대해 참석 여부를 선택합니다.
 *
 * 상태 흐름:
 * 1. 정산(정산, 회식, 정산서, 참여 멤버) 생성
 * 2. 정산 공유
 * 3. [멤버] 참여 여부 입력, 정산 열람 여부 최신화
 * 4. 멤버 확정
 * 5. 정산서 확정
 * 6. 정산서 공유
 * 7. 송금 진행
 * 8. 정산 완료
 **/
class Bill(
    val id: BillId? = null,
    val billAccount: BillAccount,
    val title: String,
    val description: String? = null,
    val hostUserId: MemberId,
    var gatheringIds: MutableList<GatheringId> = mutableListOf(),
    val completedAt: Instant? = null,
    val billStatus: BillStatus = BillStatus.OPEN,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    fun isCompleted(): Boolean = completedAt != null

    fun closeParticipation(): Bill {
        if (isCompleted()) {
            throw BillException.BillAlreadyCompletedException()
        }

        return Bill(
            id = id,
            billAccount = billAccount,
            title = title,
            hostUserId = hostUserId,
            description = description,
            completedAt = completedAt,
            billStatus = BillStatus.IN_PROGRESS,
            createdAt = createdAt,
            updatedAt = Instant.now(),
            deletedAt = deletedAt,
        )
    }

    fun checkParticipationClosable() {
        if (isCompleted()) {
            throw BillException.BillAlreadyCompletedException()
        }
        if (billStatus != BillStatus.OPEN) {
            throw BillException.BillCannotCloseParticipationException()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Bill) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    companion object {
        fun create(
            billAccount: BillAccount,
            title: String,
            description: String? = null,
            hostUserId: MemberId,
        ): Bill =
            Bill(
                billAccount = billAccount,
                title = title,
                hostUserId = hostUserId,
                description = description,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        fun findMemberBillTotalSplitAmount(
            memberId: MemberId,
            gatheringMembers: List<GatheringMember>,
            gatheringReceipts: List<GatheringReceipt>,
        ): Int? {
            val retrieveGatheringMembers = gatheringMembers.filter { it.memberId == memberId }

            val gatheringMemberPairReceipts =
                retrieveGatheringMembers.map { gatheringMember ->
                    val receipt =
                        gatheringReceipts.find { it.gatheringId == gatheringMember.gatheringId }
                            ?: throw GatheringReceiptException.GatheringReceiptNotFoundException()
                    Pair(gatheringMember, receipt)
                }
            var myTotalSplitAmount = 0

            gatheringMemberPairReceipts.filter { it.first.isJoined == true }.forEach {
                val splitAmount = it.second.splitAmount ?: return null
                myTotalSplitAmount += splitAmount
            }
            return myTotalSplitAmount
        }

        fun getBillTotalAmount(gatheringReceipts: List<GatheringReceipt>): Int = gatheringReceipts.sumOf { it.amount }

        fun getIsBillViewed(gatheringMembers: List<GatheringMember>): Boolean = gatheringMembers.any { it.isViewed }

        fun getIsBillJoined(gatheringMembers: List<GatheringMember>): Boolean? =
            if (gatheringMembers.isNotEmpty()) gatheringMembers.first().isJoined else null

        fun getIsBillInvitationSubmitted(gatheringMembers: List<GatheringMember>): Boolean =
            gatheringMembers.any { it.isInvitationSubmitted }

        fun getBillInvitedMemberCount(allBillGatheringMembers: List<GatheringMember>): Int {
            val invitedGatheringMemberSet = mutableSetOf<MemberId>()
            allBillGatheringMembers.forEach { gatheringMember ->
                invitedGatheringMemberSet.add(gatheringMember.memberId)
            }
            return invitedGatheringMemberSet.size
        }

        fun getBillInvitationSubmittedCount(allBillGatheringMembers: List<GatheringMember>): Int {
            val invitationSubmittedSet = mutableSetOf<MemberId>()
            allBillGatheringMembers.forEach { gatheringMember ->
                if (gatheringMember.isInvitationSubmitted) {
                    invitationSubmittedSet.add(gatheringMember.memberId)
                }
            }
            return invitationSubmittedSet.size
        }

        fun getBillInvitationCheckedMemberCount(allBillGatheringMembers: List<GatheringMember>): Int {
            val invitationCheckedMemberSet = mutableSetOf<MemberId>()
            allBillGatheringMembers.forEach { gatheringMember ->
                if (gatheringMember.isViewed) {
                    invitationCheckedMemberSet.add(gatheringMember.memberId)
                }
            }
            return invitationCheckedMemberSet.size
        }
    }

    override fun toString(): String =
        "Bill(id=$id," +
            "title='$title'," +
            "description=$description," +
            "hostUserId=$hostUserId," +
            "gatheringIds=$gatheringIds," +
            "completedAt=$completedAt," +
            "billStatus=$billStatus," +
            "createdAt=$createdAt," +
            "updatedAt=$updatedAt," +
            "deletedAt=$deletedAt)"
}

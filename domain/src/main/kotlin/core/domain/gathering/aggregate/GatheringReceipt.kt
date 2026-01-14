package core.domain.gathering.aggregate

import core.domain.gathering.port.inbound.command.ReceiptCommand
import core.domain.gathering.vo.GatheringId
import core.domain.gathering.vo.GatheringReceiptId
import java.time.Instant

/**
 * Receipt는 회식 차수(Gathering) 내 실제 지출을 나타내는 영수증 정보입니다.
 *
 * - 각 Receipt는 이미지(영수증 사진), 전체 금액, 1인당 분할 금액, 확정 여부 등을 가집니다.
 * - 하나의 Gathering에 여러 개의 Receipt가 연결될 수 있습니다.
 * - 영수증 확정 여부(isCompleted)는 수정 가능 여부를 제어합니다.
 */
class GatheringReceipt(
    val id: GatheringReceiptId? = null,
    val splitAmount: Int? = null,
    val amount: Int,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
    val gatheringReceiptPhotos: MutableList<GatheringReceiptPhoto>? = mutableListOf(),
    val gatheringId: GatheringId? = null,
) {
    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    fun isDeleted(): Boolean = deletedAt != null

    /**
     * 회식 정산서의 인당 부담 금액을 계산하고 수정 시각을 갱신합니다.
     *
     * @param joinMemberCount 참여 인원 수
     * @author LeeHanEum
     * @since 2025.09.13
     */
    fun closeParticipation(joinMemberCount: Int) =
        GatheringReceipt(
            id = id,
            splitAmount = (amount / joinMemberCount),
            amount = amount,
            createdAt = createdAt,
            updatedAt = Instant.now(),
            deletedAt = deletedAt,
            gatheringReceiptPhotos = gatheringReceiptPhotos,
            gatheringId = gatheringId,
        )

    /**
     * 회식 정산서의 참여 인원 수가 0 이하인지 검증합니다.
     *
     * @author LeeHanEum
     * @since 2025.09.13
     */
    fun validateJoinMemberCount(joinMemberCount: Int) = joinMemberCount <= 0

    fun isExistsSplitAmount() = splitAmount != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GatheringReceipt) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    override fun toString(): String =
        "Receipt(id=$id, splitAmount=$splitAmount, amount=$amount, createdAt=$createdAt, updatedAt=$updatedAt, " +
            "deletedAt=$deletedAt, receiptPhotos=$gatheringReceiptPhotos, gatheringId=$gatheringId)"

    companion object {
        fun create(
            receiptCommand: ReceiptCommand,
            gatheringId: GatheringId,
        ): GatheringReceipt =
            GatheringReceipt(
                amount = receiptCommand.amount,
                gatheringId = gatheringId,
            )
    }
}

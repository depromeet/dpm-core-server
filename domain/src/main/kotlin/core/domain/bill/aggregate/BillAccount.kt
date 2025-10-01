package core.domain.bill.aggregate

import core.domain.bill.enums.AccountType
import core.domain.bill.vo.BillAccountId
import java.time.Instant

/**
 * 송금 받을 계좌 정보를 나타내는 도메인 모델입니다.
 *
 * - 회식 정산 시 송금 대상 계좌로 사용됩니다.
 * - 1차 MVP에서는 운영진 계정으로 고정되어 사용됩니다.
 * - 계좌 정보는 URL(카카오페이 링크) 또는 일반 계좌번호 형태일 수 있습니다.
 */
class BillAccount(
    val id: BillAccountId? = null,
    val billAccountValue: String = "",
    val accountHolderName: String = "",
    val bankName: String = "",
    val accountType: AccountType = AccountType.ACCOUNT,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val bills: MutableList<Bill>? = mutableListOf(),
) {
    fun isDeleted(): Boolean = deletedAt != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BillAccount) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + billAccountValue.hashCode()
        return result
    }
}

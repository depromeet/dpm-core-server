package com.server.dpmcore.bill.bill.domain.model

import com.server.dpmcore.bill.billAccount.domain.BillAccount
import com.server.dpmcore.gathering.gathering.domain.model.Gathering
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
    val id: Long? = null,
    val billAccount: BillAccount? = null,
    val gatherings: List<Gathering> = listOf(),
    val completedAt: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val deletedAt: Instant? = null,
) {

    fun isCompleted(): Boolean = completedAt != null

    fun complete(now: Instant): Bill {
        if (isCompleted()) {
            throw IllegalStateException("이미 확정된 정산입니다. 수정이 불가능합니다.")
        }

        return Bill(
            id = id,
            billAccount = billAccount,
            gatherings = gatherings,
            completedAt = now,
            createdAt = createdAt,
            updatedAt = now,
            deletedAt = deletedAt
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Bill) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}

package com.server.dpmcore.bill.bill.domain.model

import com.server.dpmcore.bill.bill.domain.model.BillStatus.COMPLETED
import com.server.dpmcore.bill.bill.domain.model.BillStatus.IN_PROGRESS
import com.server.dpmcore.bill.bill.domain.model.BillStatus.OPEN
import com.server.dpmcore.bill.bill.domain.model.BillStatus.PENDING

/**
 * 정산의 상태를 나타냅니다.
 *
 * @property PENDING : 정산이 생성되어 아직 활성화되지 않은 상태입니다.
 * @property OPEN : 정산이 오픈되어 참여를 기다리고 있는 상태입니다.
 *  - 해당 단계에서 참여자들은 참여할 수 있습니다.
 *  - 인원이 확정되기 전까지의 상태입니다.
 * @property IN_PROGRESS : 정산이 진행중인 상태입니다.
 *  - 인원이 확정되어 정산을 진행중인 상태입니다.
 *  - 인원이 확정되었기에 인원을 변경할 수 없습니다.
 *  - 정산이 완료되기 전까지의 상태입니다.
 * @property COMPLETED : 정산이 완료된 상태입니다.
 */
enum class BillStatus(
    val value: String,
) {
    PENDING("참여 대기 중"),
    OPEN("참여 중"),
    IN_PROGRESS("정산 중"),
    COMPLETED("정산 완료"),
}

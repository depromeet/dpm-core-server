package core.domain.absencereason.enums

/**
 * 결석 사유서 검토 상태
 *
 * - [PENDING]: 운영진 검토를 기다리는 상태. 제출 즉시 승인으로 바뀐 뒤로는 새로 생기지 않는다.
 * - [APPROVED]: 승인된 상태. 디퍼가 제출하면 바로 이 상태가 된다.
 * - [REJECTED]: 운영진이 사유를 반려한 상태
 */
enum class AbsenceReasonStatus {
    PENDING,
    APPROVED,
    REJECTED,
}

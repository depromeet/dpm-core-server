package core.domain.absencereason.enums

/**
 * 결석 사유서 검토 상태
 *
 * - [PENDING]: 디퍼가 제출하여 운영진 검토를 기다리는 상태
 * - [APPROVED]: 운영진이 사유를 승인한 상태
 * - [REJECTED]: 운영진이 사유를 반려한 상태
 */
enum class AbsenceReasonStatus {
    PENDING,
    APPROVED,
    REJECTED,
}

package core.domain.attendance.port.outbound.query

/**
 * 한 멤버의 한 기수 출석 집계. 수료 판정의 입력이다.
 *
 * 삭제된 세션과 삭제된 출석 기록은 제외한다. 레거시 조퇴(EARLY_LEAVE)와 미인증(PENDING)은 어느 카운트에도 넣지 않는다.
 *
 * @property totalSessionCount 해당 기수에 운영진이 등록한, 삭제되지 않은 전체 세션 수(아직 열리지 않은 세션 포함)
 * @property offlineAbsentCount 오프라인 세션의 실제 결석(ABSENT) 수. 지각은 포함하지 않는다.
 */
data class AttendanceSummaryQueryModel(
    val totalSessionCount: Int,
    val presentCount: Int,
    val lateCount: Int,
    val excusedAbsentCount: Int,
    val onlineAbsentCount: Int,
    val offlineAbsentCount: Int,
) {
    val absentCount: Int
        get() = onlineAbsentCount + offlineAbsentCount
}

package core.domain.attendance.port.outbound.query

/**
 * 한 멤버의 한 기수 출석 집계(수료 판정 입력). 삭제된 세션/기록은 빼고 미인증과 레거시 조퇴는 세지 않는다.
 *
 * @property totalSessionCount 기수의 삭제되지 않은 전체 세션 수(아직 열리지 않은 세션 포함)
 * @property offlineAbsentCount 오프라인 세션의 실제 결석 수(지각 제외)
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

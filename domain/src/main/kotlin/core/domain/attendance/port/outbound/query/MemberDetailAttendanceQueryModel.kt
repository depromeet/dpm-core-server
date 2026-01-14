package core.domain.attendance.port.outbound.query

data class MemberDetailAttendanceQueryModel(
    val memberId: Long,
    val memberName: String,
    val teamNumber: Int,
    val part: String,
    val presentCount: Int,
    val lateCount: Int,
    val excusedAbsentCount: Int,
    val onlineAbsentCount: Int,
    val offlineAbsentCount: Int,
    val earlyLeaveCount: Int,
)

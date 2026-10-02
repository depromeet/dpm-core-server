package core.application.attendance.presentation.response

import core.domain.team.vo.TeamNumber
import java.time.LocalDateTime

data class DetailMemberAttendancesResponse(
    val member: DetailMemberInfo,
    val attendance: MemberDetailAttendanceCountInfo,
    val sessions: List<MemberDetailSessionInfo>,
)

data class DetailMemberInfo(
    val id: Long,
    val name: String,
    val teamNumber: TeamNumber,
    val isAdmin: Boolean,
    val part: String?,
    val attendanceStatus: String,
)

data class MemberDetailAttendanceCountInfo(
    val presentCount: Int,
    val lateCount: Int,
    val excusedAbsentCount: Int,
    val absentCount: Int,
)

data class MemberDetailSessionInfo(
    val id: Long,
    val week: Int,
    val eventName: String,
    val date: LocalDateTime,
    val attendanceStatus: String,
    val isOnline: Boolean,
    /** 이 세션에 제출한 결석 사유서. 없으면 null */
    val absenceReason: MemberDetailAbsenceReasonInfo?,
)

data class MemberDetailAbsenceReasonInfo(
    val id: Long,
    val contents: String,
    /** 검토 상태: PENDING, APPROVED, REJECTED */
    val status: String,
    /** 첨부 이미지 id, 표시 순서대로. 없으면 빈 목록 */
    val imageIds: List<Long>,
)

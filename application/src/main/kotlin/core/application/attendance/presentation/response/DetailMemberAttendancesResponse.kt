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
    /** 실제 출석 인증 시각(Asia/Seoul). 인증하지 않았거나 운영진이 상태를 바꿨으면 null */
    val attendedAt: LocalDateTime?,
    val isOnline: Boolean,
    /** 세션 장소. 온라인이면 "온라인", 오프라인이면 저장된 장소명 */
    val place: String,
    /** 이 세션에 제출한 결석 사유서. 없으면 null */
    val absenceReason: MemberDetailAbsenceReasonInfo?,
)

data class MemberDetailAbsenceReasonInfo(
    val id: Long,
    val contents: String,
    /** 검토 상태: PENDING, APPROVED, REJECTED */
    val status: String,
    /** 첨부 이미지 id, 표시 순서대로. 없으면 빈 목록. images 와 같은 순서이며 호환을 위해 유지 */
    val imageIds: List<Long>,
    /** 첨부 이미지와 원본 파일명, 표시 순서대로. 없으면 빈 목록 */
    val images: List<MemberDetailAbsenceReasonImageInfo>,
)

data class MemberDetailAbsenceReasonImageInfo(
    val imageId: Long,
    /** 업로드할 때 받은 원본 파일명. 파일명 없이 올렸거나 기능 도입 전 이미지는 null */
    val fileName: String?,
)

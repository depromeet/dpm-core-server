package core.application.attendance.presentation.request

import core.domain.member.vo.MemberId

data class AttendanceStatusBulkUpdateRequest(
    val attendanceStatus: String,
    val memberIds: List<Long>,
) {
    fun toMemberIds(): List<MemberId> = memberIds.map { MemberId(it) }
}


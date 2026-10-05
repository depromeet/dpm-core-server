package core.application.attendance.presentation.request

import core.domain.attendance.enums.AttendanceStatus
import core.domain.member.vo.MemberId
import jakarta.validation.constraints.NotEmpty

/** 지원하지 않는 상태 문자열은 역직렬화에서 400 으로 거절된다. */
data class AttendanceStatusBulkUpdateRequest(
    val attendanceStatus: AttendanceStatus,
    @field:NotEmpty
    val memberIds: List<Long>,
) {
    fun toMemberIds(): List<MemberId> = memberIds.map { MemberId(it) }
}

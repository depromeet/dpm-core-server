package core.application.attendance.presentation.request

import core.domain.member.vo.MemberId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class AttendanceStatusBulkUpdateRequest(
    @field:NotBlank
    val attendanceStatus: String,

    @field:NotEmpty
    val memberIds: List<Long>,
) {
    fun toMemberIds(): List<MemberId> = memberIds.map { MemberId(it) }
}


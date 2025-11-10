package core.application.attendance.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(
    JsonInclude.Include.NON_NULL,
)
data class MemberAttendancesResponse(
    val members: List<MemberAttendanceResponse>,
    val filter: MyTeamFilterResponse,
    val hasNext: Boolean,
    val nextCursorId: Long?,
    val totalElements: Int,
)

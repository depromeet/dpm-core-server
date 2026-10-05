package core.application.attendance.presentation.request

import core.domain.attendance.enums.AttendanceStatus

/** 지원하지 않는 상태 문자열은 역직렬화에서 400 으로 거절된다. */
data class AttendanceStatusUpdateRequest(
    val attendanceStatus: AttendanceStatus,
)

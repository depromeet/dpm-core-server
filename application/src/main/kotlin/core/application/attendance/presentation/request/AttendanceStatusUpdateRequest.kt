package core.application.attendance.presentation.request

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import core.domain.attendance.enums.AttendanceStatus

/** 상태 이름만 받으며, 지원하지 않는 상태나 숫자는 역직렬화에서 400 으로 거절된다. */
data class AttendanceStatusUpdateRequest(
    @JsonDeserialize(using = AttendanceStatusNameDeserializer::class)
    val attendanceStatus: AttendanceStatus,
)

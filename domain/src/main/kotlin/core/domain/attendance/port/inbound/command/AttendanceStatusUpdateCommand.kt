package core.domain.attendance.port.inbound.command

import core.domain.attendance.enums.AttendanceStatus
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId

data class AttendanceStatusUpdateCommand(
    val sessionId: SessionId,
    val memberId: MemberId,
    val attendanceStatus: AttendanceStatus,
)

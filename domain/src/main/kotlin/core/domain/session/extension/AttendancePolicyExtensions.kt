package core.domain.session.extension

import core.domain.session.port.inbound.command.SessionUpdateCommand
import core.domain.session.vo.AttendancePolicy

fun AttendancePolicy.hasChangedComparedTo(command: SessionUpdateCommand): Boolean {
    return this.attendanceStart != command.attendanceStart ||
            this.lateStart != command.lateStart ||
            this.absentStart != command.absentStart
}

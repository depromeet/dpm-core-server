package core.application.attendance.application.event.listener

import core.application.attendance.application.service.AttendanceCommandService
import core.domain.session.event.SessionUpdateEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SessionUpdateEventListener(
    private val attendanceCommandService: AttendanceCommandService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleSessionUpdate(event: SessionUpdateEvent) {
        attendanceCommandService.updateAttendancesByPolicy(
            sessionId = event.sessionId,
            lateStart = event.lateStart,
            absentStart = event.absentStart,
        )
    }
}

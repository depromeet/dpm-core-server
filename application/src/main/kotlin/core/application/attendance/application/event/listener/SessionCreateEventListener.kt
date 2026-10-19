package core.application.attendance.application.event.listener

import core.application.attendance.application.service.AttendanceCommandService
import core.domain.session.event.SessionCreateEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

// 세션 생성 트랜잭션 안에서 출석 기록을 만들어 함께 커밋/롤백되게 한다(AFTER_COMMIT 은 원자성을 보장하지 못한다).
@Component
class SessionCreateEventListener(
    private val attendanceCommandService: AttendanceCommandService,
) {
    @org.springframework.core.annotation.Order(0)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: SessionCreateEvent) {
        attendanceCommandService.createAttendances(
            sessionId = event.sessionId,
            cohortId = event.cohortId,
        )
    }
}

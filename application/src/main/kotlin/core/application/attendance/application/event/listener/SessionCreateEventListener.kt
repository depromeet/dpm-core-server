package core.application.attendance.application.event.listener

import core.application.attendance.application.service.AttendanceCommandService
import core.domain.session.event.SessionCreateEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 새 세션의 초기 출석 기록을 세션 생성 트랜잭션 커밋 직전에 같은 트랜잭션으로 만든다.
 * 세션, 알림 이력, 출석 기록이 함께 커밋되며, 출석 기록 생성이 실패하면 세션 생성도 롤백된다.
 * (AFTER_COMMIT 에서는 이미 끝난 트랜잭션에 참여하게 되어 쓰기가 원래 트랜잭션과 함께 커밋된다고 보장할 수 없었다.)
 */
@Component
class SessionCreateEventListener(
    private val attendanceCommandService: AttendanceCommandService,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: SessionCreateEvent) {
        attendanceCommandService.createAttendances(
            sessionId = event.sessionId,
            cohortId = event.cohortId,
        )
    }
}

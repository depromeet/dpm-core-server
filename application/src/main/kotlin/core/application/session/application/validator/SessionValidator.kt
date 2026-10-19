package core.application.session.application.validator

import core.application.session.application.exception.InvalidAttendanceCodeException
import core.application.session.application.exception.InvalidAttendanceTimeOrderException
import core.domain.session.aggregate.Session
import core.domain.session.vo.SessionAttendanceTimes
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SessionValidator {
    fun validateInputCode(
        session: Session,
        inputCode: String,
    ) {
        if (session.isInvalidInputCode(inputCode)) throw InvalidAttendanceCodeException()
    }

    /** 출석 시작 < 지각 시작 < 출석 마감 순서인지 확인합니다. 세션과 날짜가 달라도 됩니다(00:05 세션의 T-10 등). */
    fun validateAttendanceTimes(
        attendanceStart: Instant,
        lateStart: Instant,
        absentStart: Instant,
    ) {
        if (!SessionAttendanceTimes.isOrdered(attendanceStart, lateStart, absentStart)) {
            throw InvalidAttendanceTimeOrderException()
        }
    }
}

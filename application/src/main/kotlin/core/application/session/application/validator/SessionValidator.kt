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

    /**
     * 출석 시각이 출석 시작 < 지각 시작 < 출석 마감 순서인지 확인합니다.
     *
     * 세션 날짜와 같은 날일 필요는 없습니다(예: 00:05 세션의 인증 시작 T-10 은 전날 23:55).
     *
     * @throws InvalidAttendanceTimeOrderException 순서가 맞지 않을 경우
     */
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

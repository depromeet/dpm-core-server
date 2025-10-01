package core.application.session.application.validator

import core.application.session.application.exception.AttendanceStartTimeDateMismatchException
import core.application.session.application.exception.InvalidAttendanceCodeException
import core.domain.session.aggregate.Session
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
     * 출석 시작 시간이 세션과 같은 날짜인지 확인합니다.
     *
     * @param session 세션
     * @param attendanceStartTime 출석 시작 시간
     * @throws AttendanceStartTimeDateMismatchException 날짜가 다를 경우
     * @author LeeHanEum
     * @since 2025.09.13
     */
    fun validateIsSameDateAsSession(
        session: Session,
        attendanceStartTime: Instant,
    ) {
        if (!session.isSameDateAsSession(attendanceStartTime)) throw AttendanceStartTimeDateMismatchException()
    }
}

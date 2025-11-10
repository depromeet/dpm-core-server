package core.domain.session.aggregate

import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.vo.AttendanceResult
import core.domain.cohort.vo.CohortId
import core.domain.session.port.inbound.command.SessionCreateCommand
import core.domain.session.port.inbound.command.SessionUpdateCommand
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * 세션(Session) 도메인 모델
 *
 * 세션은 특정 기수의 주간 활동 정보를 나타내며, 출석 정책과 장소, 이벤트 이름 등을 포함합니다.
 *
 * 세션 생성 시에 출석 코드가 자동으로 생성됩니다.
 */
class Session(
    val id: SessionId? = null,
    val cohortId: CohortId,
    date: Instant,
    week: Int,
    private val attachments: MutableList<SessionAttachment> = mutableListOf(),
    place: String,
    eventName: String,
    isOnline: Boolean = false,
    attendancePolicy: AttendancePolicy,
    deletedAt: Instant? = null,
) {
    var date: Instant = date
        private set
    var week: Int = week
        private set
    var attendancePolicy: AttendancePolicy = attendancePolicy
        private set
    var place: String = place
        private set
    var eventName: String = eventName
        private set
    var isOnline: Boolean = isOnline
        private set
    var deletedAt: Instant? = deletedAt
        private set

    fun getAttachments(): List<SessionAttachment> = attachments.toList()

    fun attend(attendedAt: Instant): AttendanceResult = determineAttendanceStatus(attendedAt)

    fun isInvalidInputCode(inputCode: String) = inputCode != attendancePolicy.attendanceCode

    /**
     * 현재 시각을 기준으로 출석 상태를 결정하고 해당 상태를 sealed class 의 형태로 반환합니다.
     *
     * 디프만 출석 도메인 정책에 의거, 출석 시작 전 상태는 저장하지 않습니다.
     *
     * @param now 현재 시각
     * @author LeeHanEum
     * @since 2025.09.13
     *
     */
    private fun determineAttendanceStatus(now: Instant): AttendanceResult =
        when {
            now.isBefore(attendancePolicy.attendanceStart) -> AttendanceResult.TooEarly
            now.isBefore(attendancePolicy.attendanceStart.plus(16, ChronoUnit.MINUTES)) ->
                AttendanceResult.Success(AttendanceStatus.PRESENT)
            now.isBefore(attendancePolicy.attendanceStart.plus(31, ChronoUnit.MINUTES)) ->
                AttendanceResult.Success(AttendanceStatus.LATE)
            else -> AttendanceResult.Success(AttendanceStatus.ABSENT)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Session) return false

        return id == other.id && date == other.date
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + date.hashCode()
        return result
    }

    fun updateAttendanceStartTime(newStartTime: Instant) {
        this.attendancePolicy =
            attendancePolicy.copy(
                attendanceStart = newStartTime,
            )
    }

    fun isSameDateAsSession(target: Instant): Boolean {
        val zone = ZoneId.of("Asia/Seoul")
        return this.date.atZone(zone).toLocalDate() == target.atZone(zone).toLocalDate()
    }

    fun updateSession(command: SessionUpdateCommand) {
        this.date = command.date
        this.week = command.week
        this.place = command.place ?: this.place
        this.eventName = command.eventName ?: this.eventName
        this.isOnline = command.isOnline ?: this.isOnline
        this.attendancePolicy =
            AttendancePolicy(
                attendanceStart = command.attendanceStart,
                lateStart = command.lateStart,
                absentStart = command.absentStart,
                attendanceCode = this.attendancePolicy.attendanceCode,
            )
    }

    fun delete(deletedAt: Instant) {
        this.deletedAt = deletedAt
    }

    companion object {
        fun create(command: SessionCreateCommand): Session {
            fun generateAttendanceCode(): String = Random.nextInt(1000, 10000).toString()

            return Session(
                cohortId = CohortId(command.cohortId),
                date = command.date,
                week = command.week,
                place = command.place ?: "온라인",
                eventName = command.eventName ?: "${command.week}주차 세션",
                isOnline = command.isOnline ?: true,
                attendancePolicy =
                    AttendancePolicy(
                        attendanceStart = command.attendanceStart,
                        lateStart = command.lateStart,
                        absentStart = command.absentStart,
                        attendanceCode = generateAttendanceCode(),
                    ),
            )
        }
    }
}

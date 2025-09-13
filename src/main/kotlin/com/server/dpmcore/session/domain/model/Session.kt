package com.server.dpmcore.session.domain.model

import com.server.dpmcore.attendance.domain.model.AttendanceCheck
import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.session.domain.port.inbound.command.SessionCreateCommand
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * 세션(Session) 도메인 모델
 * 세션은 특정 기수의 주간 활동 정보를 나타내며, 출석 정책과 장소, 이벤트 이름 등을 포함합니다.
 * 세션 생성 시에 출석 코드가 자동으로 생성됩니다.
 */
class Session internal constructor(
    val id: SessionId? = null,
    val cohortId: CohortId,
    val date: Instant,
    val week: Int,
    private val attachments: MutableList<SessionAttachment> = mutableListOf(),
    place: String,
    eventName: String,
    isOnline: Boolean = false,
    attendancePolicy: AttendancePolicy,
) {
    var attendancePolicy: AttendancePolicy = attendancePolicy
        private set
    var place: String = place
        private set
    var eventName: String = eventName
        private set
    var isOnline: Boolean = isOnline
        private set

    fun getAttachments(): List<SessionAttachment> = attachments.toList()

    fun attend(attendedAt: Instant): AttendanceCheck = determineAttendanceStatus(attendedAt)

    fun isValidInputCode(inputCode: String) = inputCode != attendancePolicy.attendanceCode

    private fun determineAttendanceStatus(now: Instant): AttendanceCheck =
        when {
            now.isBefore(attendancePolicy.attendanceStart) -> AttendanceCheck.TooEarly
            now.isBefore(attendancePolicy.attendanceStart.plus(16, ChronoUnit.MINUTES)) ->
                AttendanceCheck.Success(AttendanceStatus.PRESENT)
            now.isBefore(attendancePolicy.attendanceStart.plus(31, ChronoUnit.MINUTES)) ->
                AttendanceCheck.Success(AttendanceStatus.LATE)
            else -> AttendanceCheck.Success(AttendanceStatus.ABSENT)
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

    companion object {
        fun create(command: SessionCreateCommand): Session {
            fun generateAttendanceCode(): String = Random.nextInt(1000, 10000).toString()

            return Session(
                cohortId = CohortId(command.cohortId),
                date = command.date.plus(command.startHour, ChronoUnit.HOURS),
                week = command.week,
                place = command.place ?: "온라인",
                eventName = command.eventName ?: "${command.week}주차 세션",
                isOnline = command.isOnline ?: true,
                attendancePolicy =
                    AttendancePolicy(
                        attendanceStart = command.date.plus(command.startHour, ChronoUnit.HOURS),
                        attendanceCode = generateAttendanceCode(),
                    ),
            )
        }
    }
}

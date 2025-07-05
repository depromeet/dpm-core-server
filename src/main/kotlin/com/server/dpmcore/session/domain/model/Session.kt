package com.server.dpmcore.session.domain.model

import com.server.dpmcore.session.domain.port.inbound.command.SessionCreateCommand
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * 세션(Session) 도메인 모델
 * 세션은 특정 기수의 주간 활동 정보를 나타내며, 출석 정책과 장소, 이벤트 이름 등을 포함합니다.
 * 세션 생성 시에 출석 코드가 자동으로 생성됩니다.
 */
class Session internal constructor(
    val id: SessionId? = null,
    val cohortId: Long,
    val date: Instant,
    val week: Int,
    val attendancePolicy: AttendancePolicy,
    place: String?,
    eventName: String?,
    isOnline: Boolean = false,
    attachments: MutableList<SessionAttachment> = mutableListOf(),
) {
    var place: String? = place
        private set
    var eventName: String? = eventName
        private set
    var isOnline: Boolean = isOnline
        private set

    private val attachments: MutableList<SessionAttachment> = mutableListOf()

    fun getAttachments(): List<SessionAttachment> = attachments.toList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Session) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + date.hashCode()
        return result
    }

    companion object {
        fun create(command: SessionCreateCommand): Session {
            fun generateAttendanceCode(): String = Random.nextInt(1000, 10000).toString()

            return Session(
                cohortId = command.cohortId,
                date = command.date,
                week = command.week,
                place = command.place ?: "온라인",
                eventName = command.eventName ?: "${command.week}주차 세션",
                isOnline = command.isOnline ?: true,
                attendancePolicy =
                    AttendancePolicy(
                        attendanceStart = command.date.plus(14, ChronoUnit.HOURS),
                        attendanceEnd = command.date.plus(14, ChronoUnit.HOURS).plus(15, ChronoUnit.MINUTES),
                        latenessStart = command.date.plus(14, ChronoUnit.HOURS).plus(15, ChronoUnit.MINUTES),
                        latenessEnd = command.date.plus(14, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES),
                        attendanceCode = generateAttendanceCode(),
                    ),
            )
        }
    }
}

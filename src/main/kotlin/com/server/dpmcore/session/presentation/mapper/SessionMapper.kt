package com.server.dpmcore.session.presentation.mapper

import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.presentation.dto.response.AttendanceTimeResponse
import com.server.dpmcore.session.presentation.dto.response.NextSessionResponse
import com.server.dpmcore.session.presentation.dto.response.SessionDetailResponse
import com.server.dpmcore.session.presentation.dto.response.SessionListDetailResponse
import com.server.dpmcore.session.presentation.dto.response.SessionListResponse
import com.server.dpmcore.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import java.time.Instant

object SessionMapper {
    fun toNextSessionResponse(session: Session): NextSessionResponse =
        with(session) {
            NextSessionResponse(
                sessionId = id?.value ?: throw IllegalStateException("Session ID cannot be null"),
                week = week,
                eventName = eventName,
                place = place,
                isOnline = isOnline,
                date = instantToLocalDateTime(date),
            )
        }

    fun toSessionListResponse(sessions: List<Session>): SessionListResponse =
        sessions.run {
            if (isEmpty()) return SessionListResponse(sessions = emptyList())

            SessionListResponse(
                sessions =
                    map {
                        SessionListDetailResponse(
                            id = it.id!!.value,
                            week = it.week,
                            eventName = it.eventName,
                            date = instantToLocalDateTime(it.date),
                        )
                    },
            )
        }

    fun toSessionDetailResponse(session: Session): SessionDetailResponse =
        with(session) {
            SessionDetailResponse(
                sessionId = id!!.value,
                week = week,
                eventName = eventName,
                place = place,
                isOnline = isOnline,
                date = instantToLocalDateTime(date),
                attendanceStartTime = session.attendancePolicy.attendanceStart.let { instantToLocalDateTime(it) },
            )
        }

    fun toAttendanceTimeResponse(attendanceStartTime: Instant) =
        AttendanceTimeResponse(
            attendanceStartTime = instantToLocalDateTime(attendanceStartTime),
        )
}

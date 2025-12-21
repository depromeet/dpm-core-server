package core.application.session.presentation.mapper

import core.application.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import core.application.session.presentation.mapper.TimeMapper.localDateTimeToInstant
import core.application.session.presentation.request.SessionCreateRequest
import core.application.session.presentation.request.SessionUpdateRequest
import core.application.session.presentation.response.AttendanceTimeResponse
import core.application.session.presentation.response.NextSessionResponse
import core.application.session.presentation.response.SessionDetailResponse
import core.application.session.presentation.response.SessionListDetailResponse
import core.application.session.presentation.response.SessionListResponse
import core.application.session.presentation.response.SessionWeekResponse
import core.application.session.presentation.response.SessionWeeksResponse
import core.domain.session.aggregate.Session
import core.domain.session.port.inbound.command.SessionAttendancePolicyCommand
import core.domain.session.port.inbound.command.SessionCreateCommand
import core.domain.session.port.inbound.command.SessionUpdateCommand
import core.domain.session.port.inbound.query.SessionWeekQueryModel
import core.domain.session.vo.SessionId
import java.time.Instant
import java.time.LocalDateTime

object SessionMapper {
    fun toNextSessionResponse(session: Session): NextSessionResponse =
        with(session) {
            NextSessionResponse(
                id = id?.value ?: throw IllegalStateException("Session ID cannot be null"),
                week = week,
                name = eventName,
                place = place,
                isOnline = isOnline,
                date = instantToLocalDateTime(date),
                attendanceCode = attendancePolicy.attendanceCode,
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
                            name = it.eventName,
                            date = instantToLocalDateTime(it.date),
                            place = it.place,
                            isOnline = it.isOnline,
                        )
                    },
            )
        }

    fun toSessionDetailResponse(session: Session): SessionDetailResponse =
        with(session) {
            SessionDetailResponse(
                id = id!!.value,
                week = week,
                name = eventName,
                place = place,
                isOnline = isOnline,
                date = instantToLocalDateTime(date),
                attendanceStart =
                    instantToLocalDateTime(session.attendancePolicy.attendanceStart),
                lateStart = instantToLocalDateTime(session.attendancePolicy.lateStart),
                absentStart = instantToLocalDateTime(session.attendancePolicy.absentStart),
                attendanceCode = session.attendancePolicy.attendanceCode,
            )
        }

    fun toAttendanceTimeResponse(attendanceStartTime: Instant) =
        AttendanceTimeResponse(
            attendanceStartTime =
                instantToLocalDateTime(attendanceStartTime),
        )

    fun toSessionCreateCommand(
        request: SessionCreateRequest,
    ) = SessionCreateCommand(
        eventName = request.name,
        date = localDateTimeToInstant(request.date),
        isOnline = request.isOnline,
        place = request.place,
        week = request.week,
        attendanceStart = localDateTimeToInstant(request.attendanceStart),
        lateStart = localDateTimeToInstant(request.lateStart),
        absentStart = localDateTimeToInstant(request.absentStart),
    )

    fun toSessionUpdateCommand(request: SessionUpdateRequest) =
        SessionUpdateCommand(
            sessionId = SessionId(request.sessionId),
            eventName = request.name,
            date = localDateTimeToInstant(request.date),
            isOnline = request.isOnline,
            place = request.place,
            week = request.week,
            attendanceStart = localDateTimeToInstant(request.attendanceStart),
            lateStart = localDateTimeToInstant(request.lateStart),
            absentStart = localDateTimeToInstant(request.absentStart),
        )

    fun toSessionWeeksResponse(model: List<SessionWeekQueryModel>): SessionWeeksResponse {
        if (model.isEmpty()) {
            return SessionWeeksResponse(
                sessions = emptyList(),
            )
        }

        return SessionWeeksResponse(
            sessions =
                model.map {
                    SessionWeekResponse(
                        id = it.sessionId,
                        week = it.week,
                        date = instantToLocalDateTime(it.date),
                    )
                },
        )
    }

    fun toSessionAttendancePolicyChangedCommand(
        sessionId: SessionId,
        attendanceStart: LocalDateTime,
        lateStart: LocalDateTime,
        absentStart: LocalDateTime,
    ) = SessionAttendancePolicyCommand(
        sessionId = sessionId,
        attendanceStart = localDateTimeToInstant(attendanceStart),
        lateStart = localDateTimeToInstant(lateStart),
        absentStart = localDateTimeToInstant(absentStart),
    )
}

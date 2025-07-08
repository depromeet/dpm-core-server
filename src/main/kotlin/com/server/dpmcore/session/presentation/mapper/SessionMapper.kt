package com.server.dpmcore.session.presentation.mapper

import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.presentation.dto.response.NextSessionResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object SessionMapper {
    fun toNextSessionResponse(session: Session): NextSessionResponse =
        NextSessionResponse(
            sessionId = session.id!!.value,
            week = session.week,
            eventName = session.eventName,
            place = session.place,
            isOnline = session.isOnline,
            date = instantToLocalDateTime(session.date),
        )

    private fun instantToLocalDateTime(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
}

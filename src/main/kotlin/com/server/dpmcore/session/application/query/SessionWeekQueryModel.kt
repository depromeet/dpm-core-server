package com.server.dpmcore.session.application.query

import com.server.dpmcore.session.domain.model.SessionId

data class SessionWeekQueryModel(
    val sessionId: SessionId,
    val week: Int,
)

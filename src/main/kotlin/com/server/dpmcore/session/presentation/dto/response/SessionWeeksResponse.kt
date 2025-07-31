package com.server.dpmcore.session.presentation.dto.response

import com.server.dpmcore.session.domain.model.SessionId

data class SessionWeeksResponse(
    val sessions: List<SessionWeekResponse>,
)

data class SessionWeekResponse(
    val id: SessionId,
    val week: Int,
)

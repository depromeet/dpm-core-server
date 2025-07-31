package com.server.dpmcore.session.presentation.dto.request

import java.time.LocalDateTime

data class SessionCreateRequest(
    val cohortId: Long,
    val date: LocalDateTime,
    val week: Int,
    val place: String,
    val eventName: String,
    val isOnline: Boolean? = false,
)

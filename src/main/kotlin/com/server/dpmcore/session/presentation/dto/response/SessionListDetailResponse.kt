package com.server.dpmcore.session.presentation.dto.response

import java.time.LocalDateTime

data class SessionListDetailResponse(
    val id: Long,
    val week: Int,
    val eventName: String,
    val date: LocalDateTime,
)

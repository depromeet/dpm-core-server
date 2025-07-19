package com.server.dpmcore.session.presentation.dto.response

data class NextSessionResponse(
    val sessionId: Long,
    val week: Int,
    val eventName: String,
    val place: String,
    val isOnline: Boolean,
    val date: String,
)

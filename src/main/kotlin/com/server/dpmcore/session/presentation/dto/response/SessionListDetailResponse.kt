package com.server.dpmcore.session.presentation.dto.response

data class SessionListDetailResponse(
    val id: Long,
    val week: Int,
    val eventName: String,
    val date: String,
)

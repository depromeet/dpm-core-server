package com.server.dpmcore.session.domain.port.inbound.command

import com.server.dpmcore.session.domain.model.SessionId

data class SessionAttachmentCreateCommand(
    val sessionId: SessionId,
    val title: String,
    val path: String,
    val idx: Int? = null,
)

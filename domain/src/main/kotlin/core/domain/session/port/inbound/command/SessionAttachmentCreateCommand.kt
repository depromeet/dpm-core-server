package core.domain.session.port.inbound.command

import core.domain.session.vo.SessionId

data class SessionAttachmentCreateCommand(
    val sessionId: SessionId,
    val title: String,
    val path: String,
    val idx: Int? = null,
)

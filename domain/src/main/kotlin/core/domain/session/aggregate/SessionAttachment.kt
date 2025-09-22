package core.domain.session.aggregate

import core.domain.session.port.inbound.command.SessionAttachmentCreateCommand
import core.domain.session.vo.SessionAttachmentId
import core.domain.session.vo.SessionId

/**
 * 세션 첨부파일(SessionAttachment) 도메인 모델
 * 세션 첨부파일은 특정 세션에 대한 파일 첨부 정보를 나타냅니다.
 * 이 모델은 세션에 첨부된 파일의 메타데이터를 관리합니다.
 */
class SessionAttachment internal constructor(
    val id: SessionAttachmentId? = null,
    val sessionId: SessionId,
    val title: String,
    val path: String,
    idx: Int? = null,
) {
    var idx: Int? = idx
        private set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionAttachment) return false

        return id == other.id && title == other.title
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + title.hashCode()
        return result
    }

    companion object {
        fun create(command: SessionAttachmentCreateCommand): SessionAttachment {
            require(command.path.isNotBlank()) { "첨부파일 경로는 비어 있을 수 없습니다." }
            require(command.title.isNotBlank()) { "첨부파일 제목은 비어 있을 수 없습니다." }

            return SessionAttachment(
                sessionId = command.sessionId,
                title = command.title,
                path = command.path,
                idx = command.idx,
            )
        }
    }
}

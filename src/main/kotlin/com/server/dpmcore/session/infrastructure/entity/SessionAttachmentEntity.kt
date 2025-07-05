package com.server.dpmcore.session.infrastructure.entity

import com.server.dpmcore.session.domain.model.SessionAttachment
import com.server.dpmcore.session.domain.model.SessionAttachmentId
import com.server.dpmcore.session.domain.model.SessionId
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "session_attachments")
class SessionAttachmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_attachment_id", nullable = false, updatable = false)
    val id: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val session: SessionEntity,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false)
    val path: String,
    val idx: Int?,
) {
    fun toDomain(): SessionAttachment =
        SessionAttachment(
            id = SessionAttachmentId(this.id),
            sessionId = SessionId(this.session.sessionId),
            title = this.title,
            path = this.path,
            idx = this.idx ?: -1,
        )

    companion object {
        fun from(
            sessionEntity: SessionEntity,
            domainModel: SessionAttachment,
        ): SessionAttachmentEntity =
            SessionAttachmentEntity(
                id = domainModel.id?.value ?: 0L,
                session = sessionEntity,
                title = domainModel.title,
                path = domainModel.path,
                idx = domainModel.idx,
            )
    }
}

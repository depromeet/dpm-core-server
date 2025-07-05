package com.server.dpmcore.session.infrastructure.entity

import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.domain.model.SessionId
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "sessions")
class SessionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id", nullable = false, updatable = false)
    val id: Long,
    val cohortId: Long,
    @Column(nullable = false)
    val date: Instant,
    @Column(nullable = false)
    val week: Int,
    @Embedded
    @Column(nullable = false)
    val attendancePolicy: EmbeddedAttendancePolicy,
    @Column(nullable = false)
    val isOnline: Boolean,
    val place: String?,
    val eventName: String?,
    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val attachments: MutableList<SessionAttachmentEntity> = mutableListOf(),
) {
    fun toDomain(): Session =
        Session(
            id = SessionId(this.id),
            cohortId = this.cohortId,
            date = this.date,
            week = this.week,
            attendancePolicy = this.attendancePolicy.toDomain(),
            isOnline = this.isOnline,
            place = this.place,
            eventName = this.eventName,
        )

    companion object {
        fun from(domainModel: Session): SessionEntity {
            val sessionEntity =
                SessionEntity(
                    id = domainModel.id?.value ?: 0L,
                    cohortId = domainModel.cohortId,
                    date = domainModel.date,
                    week = domainModel.week,
                    attendancePolicy = EmbeddedAttendancePolicy.from(domainModel.attendancePolicy),
                    isOnline = domainModel.isOnline,
                    place = domainModel.place,
                    eventName = domainModel.eventName,
                )
            val attachmentEntities =
                domainModel.getAttachments().map {
                    SessionAttachmentEntity.from(sessionEntity, it)
                }
            sessionEntity.attachments.addAll(attachmentEntities)

            return sessionEntity
        }
    }
}

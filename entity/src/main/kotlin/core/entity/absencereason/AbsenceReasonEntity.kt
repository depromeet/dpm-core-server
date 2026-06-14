package core.entity.absencereason

import core.domain.absencereason.aggregate.AbsenceReason
import core.domain.absencereason.enums.AbsenceReasonStatus
import core.domain.absencereason.vo.AbsenceReasonId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "absence_reasons")
class AbsenceReasonEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "absence_reason_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "session_id", nullable = false)
    val sessionId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(nullable = false, length = 50)
    val contents: String,
    @Column(nullable = false)
    val status: String,
    @Column(name = "created_at", nullable = true)
    val createdAt: Instant? = null,
    @Column(name = "updated_at", nullable = true)
    val updatedAt: Instant? = null,
) {
    fun toDomain(): AbsenceReason =
        AbsenceReason(
            id = AbsenceReasonId(this.id),
            sessionId = SessionId(this.sessionId),
            memberId = MemberId(this.memberId),
            contents = this.contents,
            status = AbsenceReasonStatus.valueOf(this.status),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
        )

    companion object {
        fun from(domainModel: AbsenceReason): AbsenceReasonEntity =
            AbsenceReasonEntity(
                id = domainModel.id?.value ?: 0L,
                sessionId = domainModel.sessionId.value,
                memberId = domainModel.memberId.value,
                contents = domainModel.contents,
                status = domainModel.status.name,
                createdAt = domainModel.createdAt,
                updatedAt = domainModel.updatedAt,
            )
    }
}

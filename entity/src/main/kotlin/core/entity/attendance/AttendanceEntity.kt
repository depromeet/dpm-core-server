package core.entity.attendance

import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.vo.AttendanceId
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
@Table(name = "attendances")
class AttendanceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id", nullable = false, updatable = false)
    val id: Long,
    @Column(nullable = false)
    val sessionId: Long,
    @Column(nullable = false)
    val memberId: Long,
    @Column(nullable = false)
    val status: String,
    @Column(nullable = true)
    val attendedAt: Instant? = null,
) {
    fun toDomain(): Attendance =
        Attendance(
            id = AttendanceId(this.id),
            sessionId = SessionId(this.sessionId),
            memberId = MemberId(this.memberId),
            status = AttendanceStatus.valueOf(this.status),
            attendedAt = this.attendedAt,
        )

    companion object {
        fun from(domainModel: Attendance): AttendanceEntity =
            AttendanceEntity(
                id = domainModel.id?.value ?: 0L,
                sessionId = domainModel.sessionId.value,
                memberId = domainModel.memberId.value,
                status = domainModel.status.name,
                attendedAt = domainModel.attendedAt,
            )
    }
}

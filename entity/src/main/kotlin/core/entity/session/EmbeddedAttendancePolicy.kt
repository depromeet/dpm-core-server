package core.entity.session

import core.domain.session.vo.AttendancePolicy
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.Instant

@Embeddable
class EmbeddedAttendancePolicy(
    @Column(name = "attendance_start", nullable = false)
    val attendanceStart: Instant,
    @Column(name = "attendance_code", nullable = false)
    val attendanceCode: String,
) {
    fun toDomain(): AttendancePolicy =
        AttendancePolicy(
            attendanceStart = this.attendanceStart,
            attendanceCode = this.attendanceCode,
        )

    companion object {
        fun from(domainModel: AttendancePolicy): EmbeddedAttendancePolicy =
            EmbeddedAttendancePolicy(
                attendanceStart = domainModel.attendanceStart,
                attendanceCode = domainModel.attendanceCode,
            )
    }
}

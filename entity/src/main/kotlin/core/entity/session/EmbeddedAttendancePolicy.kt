package core.entity.session

import core.domain.session.vo.AttendancePolicy
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.Instant

@Embeddable
class EmbeddedAttendancePolicy(
    @Column(name = "attendance_start", nullable = false)
    val attendanceStart: Instant,
    @Column(name = "late_start", nullable = false)
    val lateStart: Instant,
    @Column(name = "absent_start", nullable = false)
    val absentStart: Instant,
    @Column(name = "attendance_code", nullable = false)
    val attendanceCode: String,
) {
    fun toDomain(): AttendancePolicy =
        AttendancePolicy(
            attendanceStart = this.attendanceStart,
            lateStart = this.lateStart,
            absentStart = this.absentStart,
            attendanceCode = this.attendanceCode,
        )

    companion object {
        fun from(domainModel: AttendancePolicy): EmbeddedAttendancePolicy =
            EmbeddedAttendancePolicy(
                attendanceStart = domainModel.attendanceStart,
                lateStart = domainModel.lateStart,
                absentStart = domainModel.absentStart,
                attendanceCode = domainModel.attendanceCode,
            )
    }
}

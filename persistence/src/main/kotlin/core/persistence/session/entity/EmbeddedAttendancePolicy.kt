package core.persistence.session.entity

import com.server.dpmcore.session.domain.model.AttendancePolicy
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.Instant

@Embeddable
class EmbeddedAttendancePolicy(
    @Column(nullable = false)
    val attendanceStart: Instant,
    @Column(nullable = false)
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

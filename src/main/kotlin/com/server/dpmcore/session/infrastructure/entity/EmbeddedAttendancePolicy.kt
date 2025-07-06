package com.server.dpmcore.session.infrastructure.entity

import com.server.dpmcore.session.domain.model.AttendancePolicy
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.Instant

@Embeddable
class EmbeddedAttendancePolicy(
    @Column(nullable = false)
    val attendanceStart: Instant,
    @Column(nullable = false)
    val attendanceEnd: Instant,
    @Column(nullable = false)
    val latenessStart: Instant,
    @Column(nullable = false)
    val latenessEnd: Instant,
    @Column(nullable = false)
    val attendanceCode: String,
) {
    fun toDomain(): AttendancePolicy =
        AttendancePolicy(
            attendanceStart = this.attendanceStart,
            attendanceEnd = this.attendanceEnd,
            latenessStart = this.latenessStart,
            latenessEnd = this.latenessEnd,
            attendanceCode = this.attendanceCode,
        )

    companion object {
        fun from(domainModel: AttendancePolicy): EmbeddedAttendancePolicy =
            EmbeddedAttendancePolicy(
                attendanceStart = domainModel.attendanceStart,
                attendanceEnd = domainModel.attendanceEnd,
                latenessStart = domainModel.latenessStart,
                latenessEnd = domainModel.latenessEnd,
                attendanceCode = domainModel.attendanceCode,
            )
    }
}

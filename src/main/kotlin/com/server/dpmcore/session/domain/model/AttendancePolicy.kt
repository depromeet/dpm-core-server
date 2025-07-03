package com.server.dpmcore.session.domain.model

import java.time.Instant

data class AttendancePolicy(
    val attendanceStart: Instant,
    val attendanceEnd: Instant,
    val latenessStart: Instant,
    val latenessEnd: Instant,
    val attendanceCode: String,
)

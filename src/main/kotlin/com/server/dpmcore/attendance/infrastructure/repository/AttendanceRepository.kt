package com.server.dpmcore.attendance.infrastructure.repository

import com.server.dpmcore.attendance.domain.port.outbound.AttendancePersistencePort

class AttendanceRepository(
    private val attendanceJpaRepository: AttendanceJpaRepository,
) : AttendancePersistencePort

package com.server.dpmcore.attendance.infrastructure.repository

import com.server.dpmcore.attendance.domain.port.outbound.AttendancePersistencePort
import org.springframework.stereotype.Repository

@Repository
class AttendanceRepository(
    private val attendanceJpaRepository: AttendanceJpaRepository,
) : AttendancePersistencePort

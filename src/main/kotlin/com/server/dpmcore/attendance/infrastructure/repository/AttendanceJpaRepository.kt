package com.server.dpmcore.attendance.infrastructure.repository

import com.server.dpmcore.attendance.infrastructure.entity.AttendanceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceJpaRepository : JpaRepository<AttendanceEntity, Long> {
    fun findBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): AttendanceEntity?
}

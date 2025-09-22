package core.persistence.attendance.repository

import core.persistence.attendance.entity.AttendanceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceJpaRepository : JpaRepository<AttendanceEntity, Long> {
    fun findBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): AttendanceEntity?
}

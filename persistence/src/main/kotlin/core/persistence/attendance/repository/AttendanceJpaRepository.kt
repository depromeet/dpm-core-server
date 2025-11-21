package core.persistence.attendance.repository

import core.entity.attendance.AttendanceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceJpaRepository : JpaRepository<AttendanceEntity, Long> {
    fun findBySessionIdAndMemberIdAndDeletedAtIsNull(
        sessionId: Long,
        memberId: Long,
    ): AttendanceEntity?

    fun findAllBySessionIdAndDeletedAtIsNull(sessionId: Long): List<AttendanceEntity>
}

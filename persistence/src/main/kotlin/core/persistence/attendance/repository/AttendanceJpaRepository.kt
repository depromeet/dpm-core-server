package core.persistence.attendance.repository

import core.entity.attendance.AttendanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface AttendanceJpaRepository : JpaRepository<AttendanceEntity, Long> {
    fun findBySessionIdAndMemberIdAndDeletedAtIsNull(
        sessionId: Long,
        memberId: Long,
    ): AttendanceEntity?

    fun findAllBySessionIdAndDeletedAtIsNull(sessionId: Long): List<AttendanceEntity>

    // 아래 갱신은 모두 조건부 단일 UPDATE 라 다른 쓰기가 먼저 반영한 값을 덮어쓰지 않는다.

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update AttendanceEntity a set a.status = :status, a.attendedAt = :attendedAt, a.autoAbsentAt = null " +
            "where a.id = :attendanceId " +
            "and a.deletedAt is null " +
            "and a.attendedAt is null " +
            "and a.updatedAt is null " +
            "and (a.status = 'PENDING' or (a.status = 'ABSENT' and a.autoAbsentAt is not null))",
    )
    fun recordAttendanceIfAllowed(
        @Param("attendanceId") attendanceId: Long,
        @Param("status") status: String,
        @Param("attendedAt") attendedAt: Instant,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update AttendanceEntity a set a.status = :status, a.updatedAt = :updatedAt, a.autoAbsentAt = null " +
            "where a.sessionId = :sessionId " +
            "and a.memberId in :memberIds " +
            "and a.deletedAt is null",
    )
    fun updateStatusByAdmin(
        @Param("sessionId") sessionId: Long,
        @Param("memberIds") memberIds: Collection<Long>,
        @Param("status") status: String,
        @Param("updatedAt") updatedAt: Instant,
    ): Int

    @Query(
        "select count(distinct a.memberId) from AttendanceEntity a " +
            "where a.sessionId = :sessionId " +
            "and a.memberId in :memberIds " +
            "and a.deletedAt is null",
    )
    fun countActiveMembers(
        @Param("sessionId") sessionId: Long,
        @Param("memberIds") memberIds: Collection<Long>,
    ): Long

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update AttendanceEntity a set a.status = :newStatus, a.autoAbsentAt = null " +
            "where a.id = :attendanceId " +
            "and a.status = :expectedStatus " +
            "and a.attendedAt is not null " +
            "and a.updatedAt is null " +
            "and a.deletedAt is null",
    )
    fun updateStatusByPolicy(
        @Param("attendanceId") attendanceId: Long,
        @Param("expectedStatus") expectedStatus: String,
        @Param("newStatus") newStatus: String,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update AttendanceEntity a set a.status = 'PENDING', a.autoAbsentAt = null " +
            "where a.id = :attendanceId " +
            "and a.status = 'ABSENT' " +
            "and a.autoAbsentAt is not null " +
            "and a.attendedAt is null " +
            "and a.updatedAt is null " +
            "and a.deletedAt is null",
    )
    fun reopenAutoAbsence(
        @Param("attendanceId") attendanceId: Long,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update AttendanceEntity a set a.status = 'ABSENT', a.autoAbsentAt = :autoAbsentAt " +
            "where a.sessionId = :sessionId " +
            "and a.status = 'PENDING' " +
            "and a.attendedAt is null " +
            "and a.updatedAt is null " +
            "and a.deletedAt is null",
    )
    fun markAutoAbsence(
        @Param("sessionId") sessionId: Long,
        @Param("autoAbsentAt") autoAbsentAt: Instant,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update AttendanceEntity a set a.deletedAt = :deletedAt " +
            "where a.sessionId = :sessionId " +
            "and a.deletedAt is null",
    )
    fun softDeleteAllBySessionId(
        @Param("sessionId") sessionId: Long,
        @Param("deletedAt") deletedAt: Instant,
    ): Int
}

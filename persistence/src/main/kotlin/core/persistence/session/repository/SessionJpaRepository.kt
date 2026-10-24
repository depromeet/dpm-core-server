package core.persistence.session.repository

import core.entity.session.SessionEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SessionJpaRepository : JpaRepository<SessionEntity, Long> {
    fun findFirstByCohortIdAndDateGreaterThanEqualAndDeletedAtIsNullOrderByDateAsc(
        cohortId: Long,
        startOfToday: Instant,
    ): SessionEntity?

    fun findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(cohortId: Long): List<SessionEntity>

    @Query(
        "select case when count(s) > 0 then true else false end from SessionEntity s " +
            "where s.cohortId = :cohortId " +
            "and s.deletedAt is null " +
            "and s.date >= :rangeStart and s.date < :rangeEnd " +
            "and (:excludeSessionId is null or s.id <> :excludeSessionId)",
    )
    fun existsByCohortIdAndDateRange(
        @Param("cohortId") cohortId: Long,
        @Param("rangeStart") rangeStart: Instant,
        @Param("rangeEnd") rangeEnd: Instant,
        @Param("excludeSessionId") excludeSessionId: Long?,
    ): Boolean

    fun findByIdAndDeletedAtIsNull(id: Long): SessionEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionEntity s where s.id = :id and s.deletedAt is null")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): SessionEntity?

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from SessionEntity s where s.id = :id and s.deletedAt is null")
    fun findByIdForShare(
        @Param("id") id: Long,
    ): SessionEntity?

    @Query(
        "select s.id from SessionEntity s " +
            "where s.cohortId = :cohortId " +
            "and s.deletedAt is null " +
            "and s.attendancePolicy.absentStart <= :absentStartTo " +
            "and exists (" +
            "select 1 from AttendanceEntity a " +
            "where a.sessionId = s.id " +
            "and a.status = 'PENDING' " +
            "and a.deletedAt is null" +
            ") " +
            "order by s.attendancePolicy.absentStart asc, s.id asc",
    )
    fun findIdsToAutoClose(
        @Param("cohortId") cohortId: Long,
        @Param("absentStartTo") absentStartTo: Instant,
    ): List<Long>
}

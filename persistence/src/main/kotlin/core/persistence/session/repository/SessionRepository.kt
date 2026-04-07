package core.persistence.session.repository

import core.domain.cohort.vo.CohortId
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
import core.entity.session.SessionEntity
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.SESSIONS
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneId

@Repository
class SessionRepository(
    private val sessionJpaRepository: SessionJpaRepository,
    private val dsl: DSLContext,
) : SessionPersistencePort {
    override fun save(session: Session): Session = sessionJpaRepository.save(SessionEntity.from(session)).toDomain()

    override fun findNextSessionBy(startOfToday: Instant): Session? =
        sessionJpaRepository.findFirstByDateAfterAndDeletedAtIsNullOrderByDateAsc(startOfToday)?.toDomain()

    override fun findAllCohortSessions(cohortId: Long): List<Session> =
        sessionJpaRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(cohortId).map { it.toDomain() }

    override fun findSessionById(sessionId: Long): Session? =
        sessionJpaRepository.findByIdAndDeletedAtIsNull(sessionId)?.toDomain()

    override fun findSessionsWithAttendanceStartTimeBetween(
        cohortId: CohortId,
        startTime: Instant,
        endTime: Instant,
    ): List<Session> =
        dsl
            .selectFrom(SESSIONS)
            .where(SESSIONS.COHORT_ID.eq(cohortId.value))
            .and(SESSIONS.ATTENDANCE_START.ge(startTime))
            .and(SESSIONS.ATTENDANCE_START.le(endTime))
            .and(SESSIONS.DELETED_AT.isNull)
            .fetch()
            .map { record ->
                Session(
                    id = SessionId(record.sessionId!!),
                    cohortId = CohortId(record.cohortId!!),
                    date = record.date!!,
                    week = record.week!!,
                    place = record.place!!,
                    eventName = record.eventName!!,
                    isOnline = record.isOnline!!,
                    attendancePolicy =
                        AttendancePolicy(
                            attendanceStart = record.attendanceStart!!,
                            lateStart = record.lateStart!!.atZone(ZoneId.of("UTC")).toInstant(),
                            absentStart = record.absentStart!!.atZone(ZoneId.of("UTC")).toInstant(),
                            attendanceCode = record.attendanceCode!!,
                        ),
                    deletedAt = record.deletedAt?.atZone(ZoneId.of("UTC"))?.toInstant(),
                )
            }
}

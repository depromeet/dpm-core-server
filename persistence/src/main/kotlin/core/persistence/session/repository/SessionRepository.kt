package core.persistence.session.repository

import core.domain.cohort.vo.CohortId
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.AttendancePolicy
import core.domain.session.vo.SessionId
import core.entity.session.SessionEntity
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.SESSIONS
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class SessionRepository(
    private val sessionJpaRepository: SessionJpaRepository,
    private val dsl: DSLContext,
) : SessionPersistencePort {
    override fun save(session: Session): Session = sessionJpaRepository.save(SessionEntity.from(session)).toDomain()

    override fun findNextSessionBy(
        cohortId: Long,
        startOfToday: Instant,
    ): Session? =
        sessionJpaRepository
            .findFirstByCohortIdAndDateGreaterThanEqualAndDeletedAtIsNullOrderByDateAsc(cohortId, startOfToday)
            ?.toDomain()

    override fun findAllCohortSessions(cohortId: Long): List<Session> =
        sessionJpaRepository
            .findAllByCohortIdAndDeletedAtIsNullOrderByWeekAscDateAscIdAsc(cohortId)
            .map { it.toDomain() }

    override fun existsByCohortIdAndDateRange(
        cohortId: Long,
        rangeStartInclusive: Instant,
        rangeEndExclusive: Instant,
        excludeSessionId: Long?,
    ): Boolean =
        sessionJpaRepository.existsByCohortIdAndDateRange(
            cohortId,
            rangeStartInclusive,
            rangeEndExclusive,
            excludeSessionId,
        )

    override fun findSessionById(sessionId: Long): Session? =
        sessionJpaRepository.findByIdAndDeletedAtIsNull(sessionId)?.toDomain()

    override fun findSessionByIdForUpdate(sessionId: Long): Session? =
        sessionJpaRepository.findByIdForUpdate(sessionId)?.toDomain()

    override fun findSessionByIdForShare(sessionId: Long): Session? =
        sessionJpaRepository.findByIdForShare(sessionId)?.toDomain()

    override fun findSessionIdsToAutoClose(
        cohortId: CohortId,
        absentStartTo: Instant,
    ): List<SessionId> =
        sessionJpaRepository
            .findIdsToAutoClose(cohortId.value, absentStartTo)
            .map { SessionId(it) }

    /**
     * JPA는 Instant를 `datetime`에 UTC wall clock으로 저장한다.
     * jOOQ가 connection TZ(Asia/Seoul)로 Instant를 바인딩·매핑하면 출석 알림이 9시간 앞서 나간다.
     * API(JPA)와 동일하게 datetime 숫자를 UTC Instant로 해석한다.
     */
    override fun findSessionsWithAttendanceStartTimeBetween(
        cohortId: CohortId,
        startTime: Instant,
        endTime: Instant,
    ): List<Session> {
        val attendanceStart = SESSIONS.ATTENDANCE_START.coerce(SQLDataType.LOCALDATETIME(6))
        val sessionDate = SESSIONS.DATE.coerce(SQLDataType.LOCALDATETIME(6))
        val startUtc = toUtcLocalDateTime(startTime)
        val endUtc = toUtcLocalDateTime(endTime)

        return dsl
            .selectFrom(SESSIONS)
            .where(SESSIONS.COHORT_ID.eq(cohortId.value))
            .and(attendanceStart.ge(startUtc))
            .and(attendanceStart.le(endUtc))
            .and(SESSIONS.DELETED_AT.isNull)
            .fetch()
            .map { record ->
                Session(
                    id = SessionId(record.sessionId!!),
                    cohortId = CohortId(record.cohortId!!),
                    date = toUtcInstant(record.get(sessionDate)!!),
                    week = record.week!!,
                    place = record.place!!,
                    eventName = record.eventName!!,
                    isOnline = record.isOnline!!,
                    attendancePolicy =
                        AttendancePolicy(
                            attendanceStart = toUtcInstant(record.get(attendanceStart)!!),
                            lateStart = toUtcInstant(record.lateStart!!),
                            absentStart = toUtcInstant(record.absentStart!!),
                            attendanceCode = record.attendanceCode!!,
                        ),
                    deletedAt = record.deletedAt?.let(::toUtcInstant),
                )
            }
    }

    private fun toUtcLocalDateTime(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

    private fun toUtcInstant(localDateTime: LocalDateTime): Instant =
        localDateTime.atZone(ZoneOffset.UTC).toInstant()
}

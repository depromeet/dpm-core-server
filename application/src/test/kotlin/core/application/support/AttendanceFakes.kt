package core.application.support

import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.query.GetAttendancesBySessionWeekQuery
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.attendance.port.outbound.query.MemberAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberSessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MyDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionDetailAttendanceQueryModel
import core.domain.attendance.vo.AttendanceId
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import core.domain.team.vo.TeamNumber
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * 출석 행 저장소 가짜 구현. 실제 JPQL 조건부 UPDATE 와 같은 조건으로 동작하며, 각 갱신은 원자적이다.
 * (DB 잠금/격리 수준 동작은 MySQL 통합 테스트에서 검증한다.)
 */
class FakeAttendancePersistencePort : AttendancePersistencePort {
    data class Row(
        val id: Long,
        val sessionId: Long,
        val memberId: Long,
        var status: AttendanceStatus,
        var attendedAt: Instant? = null,
        var updatedAt: Instant? = null,
        var deletedAt: Instant? = null,
        var autoAbsentAt: Instant? = null,
    ) {
        fun toDomain(): Attendance =
            Attendance(
                id = AttendanceId(id),
                sessionId = SessionId(sessionId),
                memberId = MemberId(memberId),
                status = status,
                attendedAt = attendedAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                autoAbsentAt = autoAbsentAt,
            )
    }

    private val sequence = AtomicLong(0)
    private val rows = linkedMapOf<Long, Row>()

    /** 모든 쓰기 호출 기록 (아무것도 바꾸지 않았는지 확인할 때 사용) */
    val writeCalls = mutableListOf<String>()

    @Synchronized
    fun insert(
        sessionId: Long,
        memberId: Long,
        status: AttendanceStatus = AttendanceStatus.PENDING,
        attendedAt: Instant? = null,
        updatedAt: Instant? = null,
        deletedAt: Instant? = null,
        autoAbsentAt: Instant? = null,
    ): Long {
        val id = sequence.incrementAndGet()
        rows[id] = Row(id, sessionId, memberId, status, attendedAt, updatedAt, deletedAt, autoAbsentAt)
        return id
    }

    @Synchronized
    fun row(id: Long): Row = rows.getValue(id).copy()

    @Synchronized
    fun rowOf(
        sessionId: Long,
        memberId: Long,
    ): Row = rows.values.first { it.sessionId == sessionId && it.memberId == memberId }.copy()

    @Synchronized
    override fun findAttendanceBy(
        sessionId: Long,
        memberId: Long,
    ): Attendance? =
        rows.values
            .firstOrNull { it.sessionId == sessionId && it.memberId == memberId && it.deletedAt == null }
            ?.toDomain()

    @Synchronized
    override fun save(attendance: Attendance) {
        writeCalls += "save"
        val id = attendance.id?.value ?: sequence.incrementAndGet()
        rows[id] =
            Row(
                id = id,
                sessionId = attendance.sessionId.value,
                memberId = attendance.memberId.value,
                status = attendance.status,
                attendedAt = attendance.attendedAt,
                updatedAt = attendance.updatedAt,
                deletedAt = attendance.deletedAt,
                autoAbsentAt = attendance.autoAbsentAt,
            )
    }

    @Synchronized
    override fun saveInBatch(attendances: List<Attendance>) {
        writeCalls += "saveInBatch"
        attendances.forEach {
            insert(
                it.sessionId.value,
                it.memberId.value,
                it.status,
                it.attendedAt,
                it.updatedAt,
                it.deletedAt,
                it.autoAbsentAt,
            )
        }
    }

    @Synchronized
    fun all(): List<Row> = rows.values.map { it.copy() }

    @Synchronized
    override fun findAllBySessionId(sessionId: Long): List<Attendance> =
        rows.values.filter { it.sessionId == sessionId && it.deletedAt == null }.map { it.toDomain() }

    @Synchronized
    override fun recordAttendanceIfAllowed(
        attendanceId: Long,
        status: AttendanceStatus,
        attendedAt: Instant,
    ): Boolean {
        writeCalls += "recordAttendanceIfAllowed"
        val row = rows[attendanceId] ?: return false
        val allowed =
            row.deletedAt == null &&
                row.attendedAt == null &&
                row.updatedAt == null &&
                (
                    row.status == AttendanceStatus.PENDING ||
                        (row.status == AttendanceStatus.ABSENT && row.autoAbsentAt != null)
                )
        if (!allowed) return false
        row.status = status
        row.attendedAt = attendedAt
        row.autoAbsentAt = null
        return true
    }

    @Synchronized
    override fun updateStatusByAdmin(
        sessionId: Long,
        memberIds: List<Long>,
        status: AttendanceStatus,
        updatedAt: Instant,
    ): Int {
        writeCalls += "updateStatusByAdmin"
        val targets =
            rows.values.filter { it.sessionId == sessionId && it.memberId in memberIds && it.deletedAt == null }
        targets.forEach {
            it.status = status
            it.updatedAt = updatedAt
            it.autoAbsentAt = null
        }
        return targets.size
    }

    @Synchronized
    override fun countActiveAttendances(
        sessionId: Long,
        memberIds: List<Long>,
    ): Int =
        rows.values
            .filter { it.sessionId == sessionId && it.memberId in memberIds && it.deletedAt == null }
            .map { it.memberId }
            .distinct()
            .size

    @Synchronized
    override fun updateStatusByPolicy(
        attendanceId: Long,
        expectedStatus: AttendanceStatus,
        newStatus: AttendanceStatus,
    ): Boolean {
        writeCalls += "updateStatusByPolicy"
        val row = rows[attendanceId] ?: return false
        val allowed =
            row.status == expectedStatus && row.attendedAt != null && row.updatedAt == null && row.deletedAt == null
        if (!allowed) return false
        row.status = newStatus
        row.autoAbsentAt = null
        return true
    }

    @Synchronized
    override fun reopenAutoAbsence(attendanceId: Long): Boolean {
        writeCalls += "reopenAutoAbsence"
        val row = rows[attendanceId] ?: return false
        val allowed =
            row.status == AttendanceStatus.ABSENT &&
                row.autoAbsentAt != null &&
                row.attendedAt == null &&
                row.updatedAt == null &&
                row.deletedAt == null
        if (!allowed) return false
        row.status = AttendanceStatus.PENDING
        row.autoAbsentAt = null
        return true
    }

    @Synchronized
    override fun markAutoAbsence(
        sessionId: Long,
        autoAbsentAt: Instant,
    ): Int {
        writeCalls += "markAutoAbsence"
        val targets =
            rows.values.filter {
                it.sessionId == sessionId &&
                    it.status == AttendanceStatus.PENDING &&
                    it.attendedAt == null &&
                    it.updatedAt == null &&
                    it.deletedAt == null
            }
        targets.forEach {
            it.status = AttendanceStatus.ABSENT
            it.autoAbsentAt = autoAbsentAt
        }
        return targets.size
    }

    @Synchronized
    override fun softDeleteAllBySessionId(
        sessionId: Long,
        deletedAt: Instant,
    ): Int {
        writeCalls += "softDeleteAllBySessionId"
        val targets = rows.values.filter { it.sessionId == sessionId && it.deletedAt == null }
        targets.forEach { it.deletedAt = deletedAt }
        return targets.size
    }

    @Synchronized
    fun hasAutoAbsenceTarget(sessionId: Long): Boolean =
        rows.values.any {
            it.sessionId == sessionId &&
                it.status == AttendanceStatus.PENDING &&
                it.attendedAt == null &&
                it.updatedAt == null &&
                it.deletedAt == null
        }

    override fun findSessionAttendancesByQuery(
        query: GetAttendancesBySessionWeekQuery,
        myTeamNumber: TeamNumber,
    ): List<SessionAttendanceQueryModel> = throw UnsupportedOperationException()

    override fun findMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: TeamNumber,
    ): List<MemberAttendanceQueryModel> = throw UnsupportedOperationException()

    override fun findDetailAttendanceBySession(
        query: GetDetailAttendanceBySessionQuery,
    ): SessionDetailAttendanceQueryModel? = throw UnsupportedOperationException()

    override fun findDetailMemberAttendance(
        query: GetDetailMemberAttendancesQuery,
    ): List<MemberDetailAttendanceQueryModel> = throw UnsupportedOperationException()

    override fun findMemberSessionAttendances(
        query: GetDetailMemberAttendancesQuery,
    ): List<MemberSessionAttendanceQueryModel> = throw UnsupportedOperationException()

    override fun findMyDetailAttendanceBySession(query: GetMyAttendanceBySessionQuery): MyDetailAttendanceQueryModel? =
        throw UnsupportedOperationException()

    override fun countSessionAttendancesByQuery(
        query: GetAttendancesBySessionWeekQuery,
        myTeamNumber: TeamNumber,
    ): Int = throw UnsupportedOperationException()

    override fun countMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: TeamNumber,
    ): Int = throw UnsupportedOperationException()
}

/** 세션 저장소 가짜 구현. 조회마다 사본을 돌려줘 저장하지 않은 변경이 새지 않도록 한다. */
class FakeSessionPersistencePort(
    private val attendances: FakeAttendancePersistencePort,
) : SessionPersistencePort {
    private val sequence = AtomicLong(0)
    private val sessions = linkedMapOf<Long, Session>()

    /** 잠금 호출 기록: "update:{id}" / "share:{id}" */
    val lockCalls = mutableListOf<String>()

    @Synchronized
    fun stored(sessionId: Long): Session = copyOf(sessions.getValue(sessionId))

    @Synchronized
    fun all(): List<Session> = sessions.values.map { copyOf(it) }

    @Synchronized
    override fun save(session: Session): Session {
        val id = session.id?.value ?: sequence.incrementAndGet()
        val saved = copyOf(session, SessionId(id))
        sessions[id] = saved
        return copyOf(saved)
    }

    @Synchronized
    override fun findNextSessionBy(startOfToday: Instant): Session? =
        sessions.values
            .filter { it.deletedAt == null && it.date.isAfter(startOfToday) }
            .minByOrNull { it.date }
            ?.let { copyOf(it) }

    @Synchronized
    override fun findAllCohortSessions(cohortId: Long): List<Session> =
        sessions.values.filter { it.cohortId.value == cohortId && it.deletedAt == null }.map { copyOf(it) }

    @Synchronized
    override fun findSessionById(sessionId: Long): Session? =
        sessions[sessionId]?.takeIf { it.deletedAt == null }?.let { copyOf(it) }

    @Synchronized
    override fun findSessionByIdForUpdate(sessionId: Long): Session? {
        lockCalls += "update:$sessionId"
        return findSessionById(sessionId)
    }

    @Synchronized
    override fun findSessionByIdForShare(sessionId: Long): Session? {
        lockCalls += "share:$sessionId"
        return findSessionById(sessionId)
    }

    override fun findSessionsWithAttendanceStartTimeBetween(
        cohortId: CohortId,
        startTime: Instant,
        endTime: Instant,
    ): List<Session> = throw UnsupportedOperationException()

    @Synchronized
    override fun findSessionIdsToAutoClose(absentStartTo: Instant): List<SessionId> =
        sessions.values
            .filter { it.deletedAt == null && !it.attendancePolicy.absentStart.isAfter(absentStartTo) }
            .filter { attendances.hasAutoAbsenceTarget(it.id!!.value) }
            .sortedWith(compareBy({ it.attendancePolicy.absentStart }, { it.id!!.value }))
            .map { it.id!! }

    private fun copyOf(
        session: Session,
        id: SessionId? = session.id,
    ): Session =
        Session(
            id = id,
            cohortId = session.cohortId,
            date = session.date,
            week = session.week,
            attachments = session.getAttachments().toMutableList(),
            place = session.place,
            eventName = session.eventName,
            isOnline = session.isOnline,
            attendancePolicy = session.attendancePolicy.copy(),
            deletedAt = session.deletedAt,
        )
}

class FakeCohortPersistencePort : CohortPersistencePort {
    private val sequence = AtomicLong(0)
    private val cohorts = linkedMapOf<Long, Cohort>()

    @Synchronized
    override fun findAll(): List<Cohort> = cohorts.values.toList()

    @Synchronized
    override fun findById(cohortId: CohortId): Cohort? = cohorts[cohortId.value]

    @Synchronized
    override fun findByValue(value: String): Cohort? = cohorts.values.firstOrNull { it.value == value }

    @Synchronized
    override fun save(cohort: Cohort): Cohort {
        val id = cohort.id?.value ?: sequence.incrementAndGet()
        val saved =
            Cohort(
                id = CohortId(id),
                value = cohort.value,
                isActive = cohort.isActive,
                activatedAt = cohort.activatedAt,
                createdAt = cohort.createdAt ?: 0L,
                updatedAt = 0L,
            )
        cohorts[id] = saved
        return saved
    }

    @Synchronized
    override fun deleteById(cohortId: CohortId) {
        cohorts.remove(cohortId.value)
    }

    @Synchronized
    override fun existsByValue(value: String): Boolean = cohorts.values.any { it.value == value }

    override fun hasAnyReference(cohortId: CohortId): Boolean = false

    @Synchronized
    override fun findActive(): Cohort? = cohorts.values.firstOrNull { it.isActive }

    @Synchronized
    override fun deactivateAll() {
        cohorts.replaceAll { _, c -> Cohort(c.id, c.value, false, c.activatedAt, c.createdAt, c.updatedAt) }
    }

    @Synchronized
    override fun activate(cohortId: CohortId) {
        deactivateAll()
        val c = cohorts.getValue(cohortId.value)
        cohorts[cohortId.value] = Cohort(c.id, c.value, true, Instant.EPOCH, c.createdAt, c.updatedAt)
    }
}

class RecordingSentSessionNotificationCommandUseCase : SentSessionNotificationCommandUseCase {
    val saved = mutableListOf<SentSessionNotification>()

    override fun updateSentAt(sentSessionNotification: SentSessionNotification): SentSessionNotification =
        sentSessionNotification

    override fun save(sentSessionNotification: SentSessionNotification): SentSessionNotification {
        saved += sentSessionNotification
        return sentSessionNotification
    }
}

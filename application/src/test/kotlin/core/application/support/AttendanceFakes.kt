package core.application.support

import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import core.domain.notification.aggregate.SentSessionNotification
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/** 세션 저장소 가짜 구현. 조회마다 사본을 돌려줘 저장하지 않은 변경이 새지 않도록 한다. */
class FakeSessionPersistencePort : SessionPersistencePort {
    private val sequence = AtomicLong(0)
    private val sessions = linkedMapOf<Long, Session>()

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

    override fun findSessionsWithAttendanceStartTimeBetween(
        cohortId: CohortId,
        startTime: Instant,
        endTime: Instant,
    ): List<Session> = throw UnsupportedOperationException()

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

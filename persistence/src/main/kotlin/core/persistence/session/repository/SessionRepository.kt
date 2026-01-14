package core.persistence.session.repository

import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.entity.session.SessionEntity
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class SessionRepository(
    private val sessionJpaRepository: SessionJpaRepository,
) : SessionPersistencePort {
    override fun save(session: Session): Session = sessionJpaRepository.save(SessionEntity.from(session)).toDomain()

    override fun findNextSessionBy(startOfToday: Instant): Session? =
        sessionJpaRepository.findFirstByDateAfterAndDeletedAtIsNullOrderByDateAsc(startOfToday)?.toDomain()

    override fun findAllSessions(cohortId: Long): List<Session> =
        sessionJpaRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(cohortId).map { it.toDomain() }

    override fun findSessionById(sessionId: Long): Session? =
        sessionJpaRepository.findByIdAndDeletedAtIsNull(sessionId)?.toDomain()
}

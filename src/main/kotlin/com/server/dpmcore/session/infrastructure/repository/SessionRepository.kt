package com.server.dpmcore.session.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.listQuery
import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.common.jdsl.singleQueryOrNull
import com.server.dpmcore.session.domain.model.Session
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.domain.port.outbound.SessionPersistencePort
import com.server.dpmcore.session.infrastructure.entity.SessionEntity
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class SessionRepository(
    private val sessionJpaRepository: SessionJpaRepository,
    private val queryFactory: SpringDataQueryFactory,
) : SessionPersistencePort {
    override fun findNextSessionBy(currentTime: Instant): Session? =
        queryFactory
            .singleQueryOrNull<SessionEntity> {
                select(entity(SessionEntity::class))
                from(entity(SessionEntity::class))
                where(
                    col(SessionEntity::date).greaterThan(currentTime),
                )
                orderBy(col(SessionEntity::date).asc())
            }?.toDomain()

    override fun findAllSessions(cohortId: CohortId): List<Session> =
        queryFactory
            .listQuery<SessionEntity> {
                select(entity(SessionEntity::class))
                from(entity(SessionEntity::class))
                where(col(SessionEntity::cohortId).equal(cohortId.value))
                orderBy(col(SessionEntity::id).asc())
            }.map { it.toDomain() }

    override fun findSessionById(sessionId: SessionId): Session? =
        queryFactory
            .singleQueryOrNull<SessionEntity> {
                select(entity(SessionEntity::class))
                from(entity(SessionEntity::class))
                where(col(SessionEntity::id).equal(sessionId.value))
            }?.toDomain()

    override fun save(session: Session): Session = sessionJpaRepository.save(SessionEntity.from(session)).toDomain()

    override fun findAllSessionWeeks(cohortId: CohortId): List<Int> =
        queryFactory
            .listQuery<Int> {
                select(col(SessionEntity::week))
                from(entity(SessionEntity::class))
                where(col(SessionEntity::cohortId).equal(cohortId.value))
                orderBy(col(SessionEntity::week).asc())
            }
}

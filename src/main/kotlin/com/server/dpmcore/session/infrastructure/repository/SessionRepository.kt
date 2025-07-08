package com.server.dpmcore.session.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.server.dpmcore.common.jdsl.singleQueryOrNull
import com.server.dpmcore.session.domain.model.Session
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
}

package com.server.dpmcore.session.infrastructure.repository

import com.server.dpmcore.session.domain.port.outbound.SessionPersistencePort
import org.springframework.stereotype.Repository

@Repository
class SessionRepository(
    private val sessionJpaRepository: SessionJpaRepository,
) : SessionPersistencePort

package com.server.dpmcore.session.infrastructure.repository

import com.server.dpmcore.session.domain.model.Session
import org.springframework.data.jpa.repository.JpaRepository

interface SessionJpaRepository : JpaRepository<Session, Long>

package com.server.dpmcore.session.infrastructure.repository

import com.server.dpmcore.session.infrastructure.entity.SessionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SessionJpaRepository : JpaRepository<SessionEntity, Long>

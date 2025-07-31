package com.server.dpmcore.refreshToken.infrastructure.repository

import com.server.dpmcore.refreshToken.infrastructure.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, Long>

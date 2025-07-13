package com.server.dpmcore.refreshToken.infrastructure.repository

import com.server.dpmcore.refreshToken.domain.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenJpaRepository : JpaRepository<RefreshToken, Long> {
}

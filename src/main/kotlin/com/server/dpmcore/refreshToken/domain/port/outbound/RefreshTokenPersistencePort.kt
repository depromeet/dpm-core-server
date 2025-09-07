package com.server.dpmcore.refreshToken.domain.port.outbound

import com.server.dpmcore.refreshToken.domain.model.RefreshToken

interface RefreshTokenPersistencePort {
    fun save(refreshToken: RefreshToken): RefreshToken

    fun findByMemberId(memberId: Long): RefreshToken?

    fun findByToken(token: String): RefreshToken?

    fun deleteByMemberId(memberId: Long)
}

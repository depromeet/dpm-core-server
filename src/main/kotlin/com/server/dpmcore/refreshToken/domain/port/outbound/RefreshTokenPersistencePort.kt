package com.server.dpmcore.refreshToken.domain.port.outbound

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.refreshToken.domain.model.RefreshToken

interface RefreshTokenPersistencePort {
    fun save(refreshToken: RefreshToken): RefreshToken
    fun findByMemberId(memberId: MemberId): RefreshToken?
    fun findByToken(token: String): RefreshToken?
}

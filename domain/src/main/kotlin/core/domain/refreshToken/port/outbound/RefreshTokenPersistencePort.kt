package core.domain.refreshToken.port.outbound

import core.domain.refreshToken.aggregate.RefreshToken

interface RefreshTokenPersistencePort {
    fun save(refreshToken: RefreshToken): RefreshToken

    fun findByMemberId(memberId: Long): RefreshToken?

    fun findByToken(token: String): RefreshToken?

    fun deleteByMemberId(memberId: Long)
}

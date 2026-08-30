package core.application.refreshToken.application.service

import core.domain.member.vo.MemberId
import core.domain.refreshToken.port.inbound.RefreshTokenInvalidator
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RefreshTokenInvalidateService(
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
) : RefreshTokenInvalidator {
    override fun destroyRefreshToken(memberId: MemberId) {
        refreshTokenPersistencePort.deleteByMemberId(memberId.value)
    }

    override fun destroyByTokenHash(tokenHash: String) {
        refreshTokenPersistencePort.deleteByTokenHash(tokenHash)
    }

    override fun destroyOtherDevices(
        memberId: MemberId,
        keepTokenHash: String,
    ) {
        refreshTokenPersistencePort
            .findAllByMemberId(memberId.value)
            .filter { it.tokenHash != keepTokenHash }
            .forEach { refreshTokenPersistencePort.deleteByTokenHash(it.tokenHash) }
    }
}

package core.application.refreshToken.application.service

import core.application.refreshToken.application.support.TokenHasher
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.properties.TokenProperties
import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 리프레시 토큰 발급의 단일 진입점.
 *
 * 이전에는 로그인 경로 6곳과 재발급 1곳이 각자 "findByMemberId 후 덮어쓰기" 를 복붙하고 있었고,
 * 그 구조가 회원당 토큰 1개를 강제해 다중 기기 로그인을 막았다.
 */
@Service
class RefreshTokenIssueService(
    private val tokenProvider: JwtTokenProvider,
    private val tokenProperties: TokenProperties,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
) {
    private val logger = KotlinLogging.logger { }

    /**
     * 로그인 시 발급. 같은 기기의 이전 체인은 정리한다.
     * deviceId 가 null 이면(식별 불가) 정리하지 않고 새 행만 추가한다. 상한이 흡수한다.
     */
    @Transactional
    fun issueForLogin(
        memberId: MemberId,
        deviceId: String? = null,
    ): RefreshToken {
        deviceId?.let { refreshTokenPersistencePort.deleteByMemberIdAndDeviceId(memberId.value, it) }
        return issue(memberId, deviceId)
    }

    /**
     * 재발급(회전) 시 발급. 기존 행은 호출자가 markRotated 로 남겨 둔다.
     *
     * issueForLogin 과 분리한 이유는, 회전 경로에서 같은 기기의 이전 체인을 지우면
     * 유예 구간의 동시 요청들이 서로의 토큰을 삭제해 버리기 때문이다.
     */
    @Transactional
    fun issueForRotation(
        memberId: MemberId,
        deviceId: String?,
    ): RefreshToken = issue(memberId, deviceId)

    private fun issue(
        memberId: MemberId,
        deviceId: String?,
    ): RefreshToken {
        val plainToken = tokenProvider.generateRefreshToken(memberId.toString())
        val now = Instant.now()

        val saved =
            refreshTokenPersistencePort.save(
                RefreshToken.issue(
                    memberId = memberId,
                    plainToken = plainToken,
                    tokenHash = TokenHasher.sha256Hex(plainToken),
                    deviceId = deviceId,
                    issuedAt = now,
                    expiresAt = now.plusSeconds(tokenProperties.expirationTime.refreshToken),
                ),
            )

        enforceLimit(memberId)
        return saved
    }

    /**
     * 회원당 활성 토큰 상한. 초과분은 오래된 순으로 폐기한다.
     *
     * 회전이 끝난(rotatedAt != null) 행은 세지 않는다. 재발급 때마다 한 줄씩 쌓이므로
     * 함께 세면 자주 쓰는 기기의 회전 이력이 상한을 밀어 올려, 다른 기기의 멀쩡한 토큰을
     * 밀어내 버린다. 회전된 행은 RefreshTokenCleanupScheduler 가 유예 경과 후 정리한다.
     */
    private fun enforceLimit(memberId: MemberId) {
        val active =
            refreshTokenPersistencePort
                .findAllByMemberId(memberId.value)
                .filterNot { it.isRotated() }

        if (active.size <= MAX_TOKENS_PER_MEMBER) {
            return
        }

        active
            .sortedBy { it.issuedAt }
            .take(active.size - MAX_TOKENS_PER_MEMBER)
            .forEach { refreshTokenPersistencePort.deleteByTokenHash(it.tokenHash) }

        logger.info {
            "refresh token limit enforced: memberId=${memberId.value} " +
                "before=${active.size} after=$MAX_TOKENS_PER_MEMBER"
        }
    }

    companion object {
        private const val MAX_TOKENS_PER_MEMBER = 10
    }
}

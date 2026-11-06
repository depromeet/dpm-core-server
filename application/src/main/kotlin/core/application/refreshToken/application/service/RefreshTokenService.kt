package core.application.refreshToken.application.service

import core.application.refreshToken.application.dto.ReissueResult
import core.application.refreshToken.application.exception.TokenExpiredException
import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.refreshToken.application.exception.TokenNotFoundException
import core.application.refreshToken.application.support.TokenHasher
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtTokenConstant.REFRESH_TOKEN_CAMEL_CASE
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.oauth.token.JwtTokenResolver
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.aggregate.RefreshToken.Companion.GRACE_SECONDS
import core.domain.refreshToken.aggregate.RefreshToken.Companion.MAX_SUCCESSOR_DEPTH
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RefreshTokenService(
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val refreshTokenIssueService: RefreshTokenIssueService,
    private val tokenResolver: JwtTokenResolver,
    private val tokenInjector: JwtTokenInjector,
    private val tokenProvider: JwtTokenProvider,
    private val deviceIdResolver: DeviceIdResolver,
    private val members: MemberPersistencePort,
    private val identities: core.application.security.oauth.token.LoginIdentityValidator,
    private val mergedMembers: core.application.security.oauth.token.MergedMemberResolver,
    private val transactions: org.springframework.transaction.PlatformTransactionManager,
) {
    private val logger = KotlinLogging.logger { }

    fun reissue(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ReissueResult {
        val transaction =
            org.springframework.transaction.support.TransactionTemplate(transactions).apply {
                isolationLevel = org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED
                propagationBehavior = org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
            }
        repeat(3) {
            try {
                val (issued, identity) = transaction.execute { rotate(request) }!!
                return respond(issued, identity, response)
            } catch (_: MergeChangedDuringRotation) {
                // Release the old path's locks before discovering and locking the new path.
            }
        }
        throw TokenInvalidException()
    }

    private class MergeChangedDuringRotation : RuntimeException()

    private fun rotate(request: HttpServletRequest): Pair<RefreshToken, LoginIdentity?> {
        val (presentedToken, observed) = resolveStoredToken(request)
        val subjectId = tokenProvider.getMemberId(presentedToken)
        val resolved = mergedMembers.resolvePath(presentedToken) ?: throw TokenInvalidException()
        // Merge and refresh acquire all participating member rows in the same ascending order.
        val locked = members.lockApprovalTargets(resolved.path.sorted())
        val latest = mergedMembers.resolvePath(presentedToken) ?: throw TokenInvalidException()
        if (latest != resolved) throw MergeChangedDuringRotation()
        if (locked.map { it.memberId }.toSet() != resolved.path.toSet()) throw TokenInvalidException()
        val member = locked.singleOrNull { it.memberId == resolved.memberId }
        if (member == null || member.isDeleted || member.status == MemberStatus.WITHDRAWN) throw TokenInvalidException()
        val stored = refreshTokenPersistencePort.lockByTokenHash(observed.tokenHash) ?: throw TokenInvalidException()
        if (stored.memberId.value != resolved.memberId ||
            (observed.memberId.value != subjectId && observed.memberId.value !in resolved.path)
        ) {
            throw TokenInvalidException()
        }
        val now = Instant.now()

        if (stored.isExpired(now)) {
            throw TokenExpiredException()
        }

        if (stored.isRotated() && !stored.isWithinGrace(now, GRACE_SECONDS)) {
            // 재사용 감지. 1단계에서는 해당 체인만 폐기한다.
            // 회원 전체 토큰 폐기로의 강화는 이 로그가 충분히 낮아진 뒤 판단한다.
            logger.warn {
                "refresh token reuse detected: memberId=${stored.memberId.value} " +
                    "tokenId=${stored.tokenId} rotatedAt=${stored.rotatedAt}"
            }
            refreshTokenPersistencePort.deleteByTokenHash(stored.tokenHash)
            throw TokenInvalidException()
        }

        val loginIdentity = tokenProvider.getLoginIdentity(presentedToken)
        // A detached session grant is bound to this exact row and cannot follow another member merge.
        if (stored.continuationMemberId != resolved.memberId && !identities.isValid(resolved.memberId, loginIdentity)) {
            throw TokenInvalidException()
        }
        if (stored.successorTokenHash != null) {
            return currentSuccessor(stored, loginIdentity, now) to loginIdentity
        }

        if (!stored.isRotated()) {
            refreshTokenPersistencePort.markRotated(stored.tokenHash, now)
        }

        val deviceId = stored.deviceId ?: deviceIdResolver.peek(request)
        // 로그인 계정은 로그인 시점에만 알 수 있으므로, 제시된 리프레시 토큰의 값을 새 토큰으로 이어준다.
        val issued = refreshTokenIssueService.issueForRotation(stored.memberId, deviceId, loginIdentity)
        if (stored.continuationMemberId == resolved.memberId) {
            refreshTokenPersistencePort.grantContinuation(issued.tokenHash, resolved.memberId)
        }
        refreshTokenPersistencePort.linkSuccessor(stored.tokenHash, issued.tokenHash)
        return issued to loginIdentity
    }

    /** A delayed response must not replace the client's current token with a rotated ancestor. */
    private fun currentSuccessor(
        predecessor: RefreshToken,
        identity: LoginIdentity?,
        now: Instant,
    ): RefreshToken {
        val visited = mutableSetOf(predecessor.tokenHash)
        var nextHash = predecessor.successorTokenHash ?: throw TokenInvalidException()
        repeat(MAX_SUCCESSOR_DEPTH) {
            if (!visited.add(nextHash)) throw TokenInvalidException()
            val successor = refreshTokenPersistencePort.lockByTokenHash(nextHash) ?: throw TokenInvalidException()
            if (successor.memberId != predecessor.memberId || successor.isExpired(now) ||
                (
                    predecessor.continuationMemberId == predecessor.memberId.value &&
                        successor.continuationMemberId != predecessor.memberId.value
                ) ||
                (successor.isRotated() && !successor.isWithinGrace(now, GRACE_SECONDS))
            ) {
                throw TokenInvalidException()
            }
            val plain =
                tokenProvider.generateRefreshToken(
                    (successor.issuedMemberId ?: throw TokenInvalidException()).toString(),
                    identity,
                    successor.tokenJti ?: throw TokenInvalidException(),
                    successor.issuedAt,
                    successor.expiresAt,
                )
            if (TokenHasher.sha256Hex(plain) != successor.tokenHash) throw TokenInvalidException()
            if (!successor.isRotated()) {
                if (successor.successorTokenHash != null) throw TokenInvalidException()
                return RefreshToken(
                    tokenId = successor.tokenId, memberId = successor.memberId, tokenHash = successor.tokenHash,
                    deviceId = successor.deviceId, issuedAt = successor.issuedAt, expiresAt = successor.expiresAt,
                    plainToken = plain, tokenJti = successor.tokenJti, issuedMemberId = successor.issuedMemberId,
                )
            }
            nextHash = successor.successorTokenHash ?: throw TokenInvalidException()
        }
        throw TokenInvalidException()
    }

    private fun respond(
        issued: RefreshToken,
        loginIdentity: LoginIdentity?,
        response: HttpServletResponse,
    ): ReissueResult {
        val accessToken = tokenProvider.generateAccessToken(issued.memberId.toString(), loginIdentity, issued.tokenHash)
        tokenInjector.injectAccessToken(accessToken, response)
        tokenInjector.injectRefreshToken(issued, response)

        return ReissueResult(
            accessToken = accessToken,
            refreshToken = issued.requirePlainToken(),
            refreshTokenExpiresAt = issued.expiresAt,
        )
    }

    /**
     * Bearer 우선, 쿠키 폴백. 후보 중 저장소에 실제로 존재하는 첫 번째를 채택한다.
     *
     * "첫 번째 유효한 JWT" 를 고르면 안 된다. 클라이언트는 일반 API 인증용으로 Authorization 에
     * 액세스 토큰을 싣는데, 액세스와 리프레시는 서명 키도 클레임도 같아 validateToken 만으로는
     * 구분되지 않는다. 그래서 Bearer 의 액세스 토큰이 리프레시 토큰으로 채택되고, 쿠키에 멀쩡한
     * 리프레시 토큰이 함께 실려 있어도 TOKEN_NOT_FOUND 로 떨어진다.
     * 저장소에 있는지를 판정 기준으로 삼으면 액세스 토큰은 자연히 건너뛴다.
     *
     * 저장소에는 해시만 있으므로, 클레임(로그인 계정)을 읽을 수 있도록 채택된 평문 토큰도 함께 돌려준다.
     */
    private fun resolveStoredToken(request: HttpServletRequest): Pair<String, RefreshToken> {
        val candidates =
            tokenResolver
                .resolveRefreshTokenCandidatesFromRequest(request)
                .filter { tokenProvider.validateToken(it) }

        if (candidates.isEmpty()) {
            logDiagnostics(request)
            throw TokenInvalidException()
        }

        return candidates.firstNotNullOfOrNull { candidate ->
            refreshTokenPersistencePort.findByTokenHash(TokenHasher.sha256Hex(candidate))?.let { candidate to it }
        } ?: run {
            logDiagnostics(request)
            throw TokenNotFoundException()
        }
    }

    private fun logDiagnostics(request: HttpServletRequest) {
        logger.warn {
            "reissue failed: hasAuthorizationHeader=${request.getHeader(HttpHeaders.AUTHORIZATION) != null} " +
                "cookieCount=${request.cookies?.count { it.name == REFRESH_TOKEN_CAMEL_CASE } ?: 0} " +
                "hasDeviceCookie=${deviceIdResolver.peek(request) != null}"
        }
    }
}

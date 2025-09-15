package com.server.dpmcore.member.member.application

import com.server.dpmcore.member.member.application.exception.MemberIdRequiredException
import com.server.dpmcore.member.member.domain.model.Member
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.port.inbound.HandleMemberLoginUseCase
import com.server.dpmcore.member.member.domain.port.outbound.MemberPersistencePort
import com.server.dpmcore.member.memberAuthority.application.MemberAuthorityService
import com.server.dpmcore.member.memberOAuth.application.MemberOAuthService
import com.server.dpmcore.refreshToken.domain.model.RefreshToken
import com.server.dpmcore.refreshToken.domain.port.outbound.RefreshTokenPersistencePort
import com.server.dpmcore.security.oauth.dto.LoginResult
import com.server.dpmcore.security.oauth.dto.OAuthAttributes
import com.server.dpmcore.security.oauth.token.JwtTokenProvider
import com.server.dpmcore.security.properties.SecurityProperties
import com.server.dpmcore.security.redirect.LoginRedirectHandler
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberLoginService(
    private val memberPersistencePort: MemberPersistencePort,
    private val memberAuthorityService: MemberAuthorityService,
    private val memberOAuthService: MemberOAuthService,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val securityProperties: SecurityProperties,
    private val tokenProvider: JwtTokenProvider,
    private val environment: Environment,
    private val redirectHandler: LoginRedirectHandler,
) : HandleMemberLoginUseCase {
    @Transactional
    override fun handleLoginSuccess(
        request: HttpServletRequest,
        authAttributes: OAuthAttributes,
    ): LoginResult =
        memberPersistencePort
            .findByEmail(authAttributes.getEmail())
            ?.let { member -> handleExistingMemberLogin(request, member) }
            ?: handleUnregisteredMember(authAttributes)

    private fun generateLoginResult(
        memberId: MemberId,
        redirectUrl: String,
    ): LoginResult {
        val newToken = tokenProvider.generateRefreshToken(memberId.toString())
        val refreshToken =
            refreshTokenPersistencePort
                .findByMemberId(memberId.value)
                ?.apply { rotate(newToken) }
                ?: RefreshToken.create(memberId, newToken)
        val savedToken = refreshTokenPersistencePort.save(refreshToken)

        return LoginResult(savedToken, redirectUrl)
    }

    private fun handleExistingMemberLogin(
        request: HttpServletRequest,
        member: Member,
    ): LoginResult {
        member.id?.value ?: return LoginResult(null, securityProperties.restrictedRedirectUrl)

        if (!member.isAllowed() || memberPersistencePort.existsDeletedMemberById(member.id.value)) {
            return LoginResult(null, securityProperties.restrictedRedirectUrl)
        }

        return generateLoginResult(
            member.id,
            redirectHandler.determineRedirectUrl(
                memberAuthorityService.resolvePrimaryAuthorityType(member.id),
                request,
            ),
        )
    }

    private fun handleUnregisteredMember(authAttributes: OAuthAttributes): LoginResult {
        val member =
            memberPersistencePort.save(Member.create(authAttributes.getEmail(), authAttributes.getName(), environment))
        memberOAuthService.addMemberOAuthProvider(member, authAttributes)

        return generateLoginResult(
            member.id ?: throw MemberIdRequiredException(),
            securityProperties.restrictedRedirectUrl,
        )
    }
}

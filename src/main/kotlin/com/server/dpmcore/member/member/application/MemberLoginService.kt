package com.server.dpmcore.member.member.application

import com.server.dpmcore.member.member.application.exception.MemberIdCanNotBeNullException
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
) : HandleMemberLoginUseCase {
    @Transactional
    override fun handleLoginSuccess(authAttributes: OAuthAttributes): LoginResult =
        memberPersistencePort
            .findByEmail(authAttributes.getEmail())
            ?.let { member -> handleExistingMemberLogin(member) }
            ?: handleUnregisteredMember(authAttributes)

    private fun generateLoginResult(
        memberId: MemberId,
        redirectUrl: String,
    ): LoginResult {
        val newToken = tokenProvider.generateRefreshToken(memberId.toString())
        val refreshToken =
            refreshTokenPersistencePort
                .findByMemberId(memberId)
                ?.apply { rotate(newToken) }
                ?: RefreshToken.create(memberId, newToken)
        val savedToken = refreshTokenPersistencePort.save(refreshToken)

        return LoginResult(savedToken, redirectUrl)
    }

    private fun handleExistingMemberLogin(member: Member): LoginResult {
        member.id?.value ?: return LoginResult(null, securityProperties.restrictedRedirectUrl)

        if (!member.isAllowed() || memberPersistencePort.existsDeletedMemberById(member.id.value)) {
            return LoginResult(null, securityProperties.restrictedRedirectUrl)
        }

        val isAdmin =
            memberAuthorityService
                .getAuthorityNamesByMemberId(member.id)
                .any { it in ADMIN_AUTHORITIES }

        val redirectUrl =
            when {
                isAdmin -> securityProperties.adminRedirectUrl
                else -> securityProperties.redirectUrl
            }

        return generateLoginResult(member.id, redirectUrl)
    }

    private fun handleUnregisteredMember(authAttributes: OAuthAttributes): LoginResult {
        val member = memberPersistencePort.save(Member.create(authAttributes.getEmail(), environment))
        memberOAuthService.addMemberOAuthProvider(member, authAttributes)

        return generateLoginResult(
            member.id ?: throw MemberIdCanNotBeNullException(),
            securityProperties.restrictedRedirectUrl,
        )
    }

    companion object {
        private val ADMIN_AUTHORITIES = setOf("ORGANIZER")
    }
}

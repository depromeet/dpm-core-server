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
    override fun handleLoginSuccess(
        requestDomain: String,
        authAttributes: OAuthAttributes,
    ): LoginResult =
        memberPersistencePort
            .findByEmail(authAttributes.getEmail())
            ?.let { member -> handleExistingMemberLogin(requestDomain, member) }
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

    private fun handleExistingMemberLogin(
        requestDomain: String,
        member: Member,
    ): LoginResult {
        member.id?.value ?: return LoginResult(null, securityProperties.restrictedRedirectUrl)

        if (!member.isAllowed() || memberPersistencePort.existsDeletedMemberById(member.id.value)) {
            return LoginResult(null, securityProperties.restrictedRedirectUrl)
        }

        return generateLoginResult(member.id, buildRedirectUrlByRoleAndDomain(requestDomain, member.id))
    }

    private fun buildRedirectUrlByRoleAndDomain(
        requestDomain: String,
        memberId: MemberId,
    ) = when {
        hasAdminRole(memberId) -> adminRedirectUrl(requestDomain)
        else -> securityProperties.redirectUrl + "?$IS_ADMIN_FALSE"
    }

    private fun adminRedirectUrl(requestDomain: String): String =
        if (environment.activeProfiles.equals("local")) {
            "${securityProperties.adminRedirectUrl}?$IS_ADMIN_TRUE"
        } else {
            when (requestDomain) {
                "$CLIENT_SUFFIX.${securityProperties.cookie.domain}" ->
                    "${securityProperties.redirectUrl}?$IS_ADMIN_TRUE"
                "$ADMIN_SUFFIX.${securityProperties.cookie.domain}" ->
                    "${securityProperties.adminRedirectUrl}?$IS_ADMIN_TRUE"
                else -> "${securityProperties.adminRedirectUrl}?$IS_ADMIN_TRUE"
            }
        }

    private fun hasAdminRole(memberId: MemberId): Boolean =
        memberAuthorityService
            .getAuthorityNamesByMemberId(memberId)
            .any { it in ADMIN_AUTHORITIES }

    private fun handleUnregisteredMember(authAttributes: OAuthAttributes): LoginResult {
        val member = memberPersistencePort.save(Member.create(authAttributes.getEmail(), environment))
        memberOAuthService.addMemberOAuthProvider(member, authAttributes)

        return generateLoginResult(
            member.id ?: throw MemberIdRequiredException(),
            securityProperties.restrictedRedirectUrl,
        )
    }

    companion object {
        private val ADMIN_AUTHORITIES = setOf("ORGANIZER")
        private const val IS_ADMIN_TRUE = "isAdmin=true"
        private const val IS_ADMIN_FALSE = "isAdmin=false"
        private const val ADMIN_SUFFIX = "admin"
        private const val CLIENT_SUFFIX = "client"
    }
}

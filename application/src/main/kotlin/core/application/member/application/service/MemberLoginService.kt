package core.application.member.application.service

import core.application.common.constant.Profile
import core.application.member.application.exception.MemberIdRequiredException
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.member.application.service.role.MemberRoleService
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.properties.SecurityProperties
import core.application.security.redirect.handler.LoginRedirectHandler
import core.domain.member.aggregate.Member
import core.domain.member.port.inbound.HandleMemberLoginUseCase
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import core.domain.security.oauth.dto.LoginResult
import core.domain.security.oauth.dto.OAuthAttributes
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberLoginService(
    private val memberPersistencePort: MemberPersistencePort,
    private val memberOAuthService: MemberOAuthService,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val securityProperties: SecurityProperties,
    private val tokenProvider: JwtTokenProvider,
    private val environment: Environment,
    private val redirectHandler: LoginRedirectHandler,
    private val memberRoleService: MemberRoleService,
) : HandleMemberLoginUseCase {
    @Transactional
    override fun handleLoginSuccess(
        requestUrl: String,
        authAttributes: OAuthAttributes,
    ): LoginResult {
        val provider = authAttributes.getProvider()
        val externalId = authAttributes.getExternalId()

        // 1. OAuth 연동 계정 존재 여부 확인
        val memberOAuth =
            memberOAuthService.findByProviderAndExternalId(provider, externalId)

        if (memberOAuth != null) {
            val member =
                memberPersistencePort.findById(memberOAuth.memberId)
                    ?: throw IllegalStateException("MemberOAuth exists but Member not found")
            return handleExistingMemberLogin(requestUrl, member)
        }

        // 2. OAuth 연동 없음 → 신규 회원 플로우
        return handleUnregisteredMember(authAttributes)
    }

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
        requestUrl: String,
        member: Member,
    ): LoginResult {
        val memberId = member.id ?: return LoginResult(null, securityProperties.redirect.restrictedRedirectUrl)

        if (!member.isAllowed() || memberPersistencePort.existsDeletedMemberById(memberId.value)) {
            return LoginResult(null, securityProperties.redirect.restrictedRedirectUrl)
        }

        return generateLoginResult(
            memberId,
            redirectHandler.determineRedirectUrl(
                requestUrl,
                memberRoleService.resolvePrimaryRoleType(memberId),
                Profile.get(environment),
            ),
        )
    }

    private fun handleUnregisteredMember(authAttributes: OAuthAttributes): LoginResult {
        val member =
            memberPersistencePort.save(
                Member.create(
                    authAttributes.getEmail(),
                    authAttributes.getName(),
                    Profile.get(environment).value,
                ),
            )
        memberOAuthService.addMemberOAuthProvider(member, authAttributes)

        memberRoleService.assignGuestRole(member.id!!)

        return generateLoginResult(
            member.id ?: throw MemberIdRequiredException(),
            securityProperties.redirect.restrictedRedirectUrl,
        )
    }
}

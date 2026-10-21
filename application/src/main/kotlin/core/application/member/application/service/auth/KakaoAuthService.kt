package core.application.member.application.service.auth

import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.service.MemberIdentityLockService
import core.application.member.application.service.TrackMemberBadges
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.refreshToken.application.service.RefreshTokenIssueService
import core.application.security.oauth.kakao.KakaoUserInfoClient
import core.application.security.oauth.redirect.OAuthRedirectUriValidator
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.aggregate.Member
import core.domain.member.enums.LoginMethod
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.security.oauth.dto.OAuthAttributes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KakaoAuthService(
    private val kakaoUserInfoClient: KakaoUserInfoClient,
    private val redirectUriValidator: OAuthRedirectUriValidator,
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort,
    private val memberPersistencePort: MemberPersistencePort,
    private val identityLock: MemberIdentityLockService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenIssueService: RefreshTokenIssueService,
    private val memberRoleService: MemberRoleService,
    private val memberTeamService: MemberTeamService,
) {
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @TrackMemberBadges
    fun login(
        authorizationCode: String,
        redirectUri: String? = null,
        deviceId: String? = null,
    ): AuthTokenResponse {
        val validatedRedirectUri = redirectUri?.let { redirectUriValidator.validate(it) }
        val attributes =
            OAuthAttributes.of(
                KAKAO_PROVIDER_ID,
                kakaoUserInfoClient.getUserAttributes(
                    authorizationCode = authorizationCode,
                    redirectUri = validatedRedirectUri,
                ),
            ) ?: throw IllegalStateException("Failed to parse Kakao user attributes")

        val memberOAuth =
            memberOAuthPersistencePort.findByProviderAndExternalId(
                provider = attributes.getProvider(),
                externalId = attributes.getExternalId(),
            )

        val (member, linkedOAuth) =
            if (memberOAuth == null) {
                val targetMember = createNewMember(attributes.getEmail(), attributes.getName())

                identityLock.lockMember(requireNotNull(targetMember.id))
                validateMemberForLogin(targetMember)
                val savedOAuth =
                    try {
                        memberOAuthPersistencePort.save(
                            core.domain.member.aggregate.MemberOAuth.of(
                                externalId = attributes.getExternalId(),
                                provider = attributes.getProvider(),
                                memberId = targetMember.id!!,
                                email = attributes.getEmail(),
                            ),
                            targetMember,
                        )
                    } catch (_: org.springframework.dao.DataIntegrityViolationException) {
                        throw core.application.member.application.exception.MemberOAuthConflictException()
                    }

                targetMember to savedOAuth
            } else {
                identityLock.lockOAuth(memberOAuth)
                val targetMember =
                    memberPersistencePort.findById(memberOAuth.memberId)
                        ?: recoverOrCreateMemberForOrphanedOAuth(attributes)
                validateMemberForLogin(targetMember)
                memberOAuthPersistencePort.updateEmail(
                    provider = attributes.getProvider(),
                    externalId = attributes.getExternalId(),
                    email = attributes.getEmail(),
                )

                targetMember to memberOAuth
            }

        validateMemberForLogin(member)
        memberRoleService.ensureGuestRoleAssigned(member.id!!)
        memberTeamService.ensureMemberTeamInitialized(member.id!!)

        val loginIdentity = LoginIdentity(LoginMethod.from(attributes.getProvider()), linkedOAuth.id!!.value)
        val accessToken = jwtTokenProvider.generateAccessToken(member.id!!.toString(), loginIdentity)
        val issued = refreshTokenIssueService.issueForLogin(member.id!!, deviceId, loginIdentity)

        return AuthTokenResponse(accessToken, issued.requirePlainToken())
    }

    private fun recoverOrCreateMemberForOrphanedOAuth(attributes: OAuthAttributes): Member {
        val targetMember = createNewMember(attributes.getEmail(), attributes.getName())

        identityLock.lockMember(requireNotNull(targetMember.id))
        validateMemberForLogin(targetMember)
        memberOAuthPersistencePort.relinkToMember(
            provider = attributes.getProvider(),
            externalId = attributes.getExternalId(),
            member = targetMember,
        )

        return targetMember
    }

    private fun createNewMember(
        email: String,
        name: String,
    ): Member = memberPersistencePort.save(Member.createPending(email, name))

    private fun validateMemberForLogin(member: Member) {
        if (member.deletedAt != null || member.status == core.domain.member.enums.MemberStatus.WITHDRAWN) {
            throw MemberDeletedException()
        }
    }

    companion object {
        private const val KAKAO_PROVIDER_ID = "KAKAO"
    }
}

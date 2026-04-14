package core.application.member.application.service.auth

import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.security.oauth.kakao.KakaoTokenExchangeService
import core.application.security.oauth.redirect.OAuthRedirectUriValidator
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import core.domain.security.oauth.dto.OAuthAttributes
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KakaoAuthService(
    private val kakaoTokenExchangeService: KakaoTokenExchangeService,
    private val redirectUriValidator: OAuthRedirectUriValidator,
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort,
    private val memberPersistencePort: MemberPersistencePort,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val memberRoleService: MemberRoleService,
    private val memberTeamService: MemberTeamService,
) {
    @Transactional
    fun login(
        authorizationCode: String,
        redirectUri: String? = null,
    ): AuthTokenResponse {
        val validatedRedirectUri = redirectUri?.let { redirectUriValidator.validate(it) }
        val attributes =
            OAuthAttributes.of(
                KAKAO_PROVIDER_ID,
                kakaoTokenExchangeService.getUserAttributes(
                    authorizationCode = authorizationCode,
                    redirectUri = validatedRedirectUri,
                ),
            ) ?: throw IllegalStateException("Failed to parse Kakao user attributes")

        val memberOAuth =
            memberOAuthPersistencePort.findByProviderAndExternalId(
                provider = attributes.getProvider(),
                externalId = attributes.getExternalId(),
            )

        val member =
            if (memberOAuth == null) {
                val existingMembers = memberPersistencePort.findAllBySignupEmail(attributes.getEmail())
                val targetMember =
                    selectLoginCandidate(existingMembers)
                        ?: createOrFindMemberBySignupEmail(
                            email = attributes.getEmail(),
                            name = attributes.getName(),
                        )

                memberOAuthPersistencePort.save(
                    core.domain.member.aggregate.MemberOAuth.of(
                        externalId = attributes.getExternalId(),
                        provider = attributes.getProvider(),
                        memberId = targetMember.id!!,
                    ),
                    targetMember,
                )

                targetMember
            } else {
                memberPersistencePort.findById(memberOAuth.memberId)
                    ?: recoverOrCreateMemberForOrphanedOAuth(attributes)
            }

        validateMemberForLogin(member)
        memberRoleService.ensureGuestRoleAssigned(member.id!!)
        memberTeamService.ensureMemberTeamInitialized(member.id!!)

        val accessToken = jwtTokenProvider.generateAccessToken(member.id!!.toString())
        val refreshToken = jwtTokenProvider.generateRefreshToken(member.id!!.toString())
        val refreshTokenEntity =
            refreshTokenPersistencePort.findByMemberId(member.id!!.value)
                ?.apply { rotate(refreshToken) }
                ?: RefreshToken.create(member.id!!, refreshToken)
        refreshTokenPersistencePort.save(refreshTokenEntity)

        return AuthTokenResponse(accessToken, refreshToken)
    }

    private fun recoverOrCreateMemberForOrphanedOAuth(attributes: OAuthAttributes): Member {
        val targetMember =
            selectLoginCandidate(memberPersistencePort.findAllBySignupEmail(attributes.getEmail()))
                ?: createOrFindMemberBySignupEmail(
                    email = attributes.getEmail(),
                    name = attributes.getName(),
                )

        memberOAuthPersistencePort.relinkToMember(
            provider = attributes.getProvider(),
            externalId = attributes.getExternalId(),
            member = targetMember,
        )

        return targetMember
    }

    private fun selectLoginCandidate(members: List<Member>): Member? {
        val availableMembers = members.filter { it.deletedAt == null }
        if (availableMembers.isEmpty()) {
            return null
        }

        val activeCandidates = availableMembers.filter { it.isAllowed() }
        val eligible = if (activeCandidates.isNotEmpty()) activeCandidates else availableMembers
        return eligible.maxByOrNull { it.id?.value ?: 0L }
    }

    private fun createOrFindMemberBySignupEmail(
        email: String,
        name: String,
    ): Member =
        try {
            memberPersistencePort.save(
                Member.createPending(
                    email = email,
                    name = name,
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            selectLoginCandidate(memberPersistencePort.findAllBySignupEmail(email))
                ?: throw IllegalStateException("Member creation conflicted but no existing member found")
        }

    private fun validateMemberForLogin(member: Member) {
        if (member.deletedAt != null || member.status == core.domain.member.enums.MemberStatus.WITHDRAWN) {
            throw MemberDeletedException()
        }
    }

    companion object {
        private const val KAKAO_PROVIDER_ID = "KAKAO"
    }
}

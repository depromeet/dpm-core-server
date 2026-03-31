package core.application.member.application.service.auth

import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.security.oauth.apple.AppleTokenExchangeService
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppleAuthService(
    private val appleTokenExchangeService: AppleTokenExchangeService,
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort,
    private val memberPersistencePort: MemberPersistencePort,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val appleIdTokenValidator: core.application.security.oauth.apple.AppleIdTokenValidator,
    private val memberRoleService: MemberRoleService,
    private val memberTeamService: MemberTeamService,
) {
    @Transactional
    fun login(
        authorizationCode: String,
        fullName: String? = null,
        familyName: String? = null,
        givenName: String? = null,
    ): AuthTokenResponse {
        val tokenResponse = appleTokenExchangeService.getTokens(authorizationCode)

        val claims = appleIdTokenValidator.verify(tokenResponse.id_token)
        val externalId = claims.subject ?: throw IllegalArgumentException("Invalid ID Token: sub missing")
        val email =
            claims["email"] as? String
                ?: throw IllegalArgumentException("Invalid ID Token: email missing")

        val memberOAuth =
            memberOAuthPersistencePort.findByProviderAndExternalId(OAuthProvider.APPLE, externalId)

        val member =
            if (memberOAuth == null) {
                val existingMembers = memberPersistencePort.findAllBySignupEmail(email)
                val existingMember = selectLoginCandidate(existingMembers)
                val resolvedFullName =
                    fullName?.trim()?.takeIf { it.isNotBlank() }
                        ?: (claims["name"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                val memberName =
                    resolveMemberName(
                        fullName = resolvedFullName,
                        familyName = familyName,
                        givenName = givenName,
                        email = email,
                    )

                val targetMember =
                    existingMember
                        ?: createOrFindMemberBySignupEmail(
                            email = email,
                            name = memberName,
                        )

                memberOAuthPersistencePort.save(
                    MemberOAuth.of(
                        externalId = externalId,
                        provider = OAuthProvider.APPLE,
                        memberId = targetMember.id!!,
                    ),
                    targetMember,
                )

                targetMember
            } else {
                memberPersistencePort.findById(memberOAuth.memberId)
                    ?: recoverOrCreateMemberForOrphanedOAuth(
                        externalId = externalId,
                        email = email,
                        name =
                            resolveMemberName(
                                fullName = fullName,
                                familyName = familyName,
                                givenName = givenName,
                                email = email,
                            ),
                    )
            }

        validateMemberForLogin(member)
        memberRoleService.ensureGuestRoleAssigned(member.id!!)
        memberTeamService.ensureMemberTeamInitialized(member.id!!)

        // 4. Issue App Tokens
        val accessToken = jwtTokenProvider.generateAccessToken(member.id!!.toString())
        val refreshToken = jwtTokenProvider.generateRefreshToken(member.id!!.toString())

        // 5. Save Refresh Token
        val refreshTokenEntity =
            refreshTokenPersistencePort.findByMemberId(member.id!!.value)
                ?.apply { rotate(refreshToken) }
                ?: RefreshToken.create(member.id!!, refreshToken)
        refreshTokenPersistencePort.save(refreshTokenEntity)

        return AuthTokenResponse(accessToken, refreshToken)
    }

    private fun resolveMemberName(
        fullName: String?,
        familyName: String?,
        givenName: String?,
        email: String,
    ): String {
        val normalizedFamilyName = familyName?.trim().orEmpty()
        val normalizedGivenName = givenName?.trim().orEmpty()

        if (normalizedFamilyName.isNotBlank() || normalizedGivenName.isNotBlank()) {
            return normalizedFamilyName + normalizedGivenName
        }

        return fullName?.trim()?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").ifBlank { "Apple User" }
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

    private fun recoverOrCreateMemberForOrphanedOAuth(
        externalId: String,
        email: String,
        name: String,
    ): Member {
        val targetMember =
            selectLoginCandidate(memberPersistencePort.findAllBySignupEmail(email))
                ?: createOrFindMemberBySignupEmail(
                    email = email,
                    name = name,
                )

        memberOAuthPersistencePort.relinkToMember(
            provider = OAuthProvider.APPLE,
            externalId = externalId,
            member = targetMember,
        )

        return targetMember
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
}

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

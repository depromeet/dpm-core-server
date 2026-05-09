package core.application.member.application.service

import core.application.member.application.exception.MemberIdRequiredException
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberStatus
import core.domain.member.port.inbound.HandleMemberLoginUseCase
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import core.domain.security.oauth.dto.LoginResult
import core.domain.security.oauth.dto.OAuthAttributes
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberLoginService(
    private val memberPersistencePort: MemberPersistencePort,
    private val memberOAuthService: MemberOAuthService,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val tokenProvider: JwtTokenProvider,
    private val memberRoleService: MemberRoleService,
    private val memberTeamService: MemberTeamService,
) : HandleMemberLoginUseCase {
    @Transactional
    override fun handleLoginSuccess(authAttributes: OAuthAttributes): LoginResult {
        val provider = authAttributes.getProvider()
        val externalId = authAttributes.getExternalId()

        // 1. OAuth 연동 계정 존재 여부 확인
        val memberOAuth =
            memberOAuthService.findByProviderAndExternalId(provider, externalId)

        if (memberOAuth != null) {
            val member =
                memberPersistencePort.findById(memberOAuth.memberId)
                    ?: recoverOrCreateMemberForOrphanedOAuth(authAttributes).also {
                        memberOAuthService.relinkMemberOAuthProvider(it, authAttributes)
                    }
            return handleExistingMemberLogin(member)
        }

        // 2. OAuth 연동 없음 → 신규 회원 플로우
        return handleUnregisteredMember(authAttributes)
    }

    private fun generateLoginResult(memberId: MemberId): LoginResult {
        val newToken = tokenProvider.generateRefreshToken(memberId.toString())
        val refreshToken =
            refreshTokenPersistencePort
                .findByMemberId(memberId.value)
                ?.apply { rotate(newToken) }
                ?: RefreshToken.create(memberId, newToken)
        val savedToken = refreshTokenPersistencePort.save(refreshToken)

        return LoginResult(savedToken)
    }

    private fun handleExistingMemberLogin(member: Member): LoginResult {
        val memberId = member.id ?: return LoginResult(null)

        if (member.deletedAt == null) {
            memberTeamService.ensureMemberTeamInitialized(memberId)
        }

        if (memberPersistencePort.existsDeletedMemberById(memberId.value) || member.status == MemberStatus.WITHDRAWN) {
            return LoginResult(null)
        }

        memberRoleService.ensureGuestRoleAssigned(memberId)

        if (member.status == MemberStatus.PENDING) {
            return generateLoginResult(memberId)
        }

        return generateLoginResult(memberId)
    }

    private fun handleUnregisteredMember(authAttributes: OAuthAttributes): LoginResult {
        val existingMembers = memberPersistencePort.findAllBySignupEmail(authAttributes.getEmail())
        val member =
            if (existingMembers.isNotEmpty()) {
                selectLoginCandidate(existingMembers)
            } else {
                createOrFindMemberBySignupEmail(
                    email = authAttributes.getEmail(),
                    name = authAttributes.getName(),
                )
            }

        memberOAuthService.addMemberOAuthProvider(member, authAttributes)
        memberRoleService.ensureGuestRoleAssigned(member.id ?: throw MemberIdRequiredException())

        return handleExistingMemberLogin(member)
    }

    private fun selectLoginCandidate(members: List<Member>): Member {
        val availableMembers = members.filter { it.deletedAt == null }
        val activeCandidates = availableMembers.filter { it.isAllowed() }
        val eligible = if (activeCandidates.isNotEmpty()) activeCandidates else availableMembers
        return eligible.maxByOrNull { it.id?.value ?: 0L } ?: throw MemberIdRequiredException()
    }

    private fun recoverOrCreateMemberForOrphanedOAuth(authAttributes: OAuthAttributes): Member {
        val existingMembers = memberPersistencePort.findAllBySignupEmail(authAttributes.getEmail())
        return if (existingMembers.isNotEmpty()) {
            selectLoginCandidate(existingMembers)
        } else {
            createOrFindMemberBySignupEmail(
                email = authAttributes.getEmail(),
                name = authAttributes.getName(),
            )
        }
    }

    private fun createOrFindMemberBySignupEmail(
        email: String,
        name: String,
    ): Member =
        try {
            memberPersistencePort.save(
                Member.createPending(
                    email,
                    name,
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            selectLoginCandidate(memberPersistencePort.findAllBySignupEmail(email))
        }
}

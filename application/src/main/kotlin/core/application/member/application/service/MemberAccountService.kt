package core.application.member.application.service

import core.application.member.application.exception.MemberAccountException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberOAuthConflictException
import core.application.member.presentation.response.ConnectedMemberAccount
import core.application.member.presentation.response.MemberAccountResponse
import core.application.security.oauth.token.LoginIdentityValidator
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.LoginMethod
import core.domain.member.port.outbound.MemberAccountPersistencePort
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.domain.security.oauth.dto.OAuthAttributes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberAccountService(
    private val members: MemberPersistencePort,
    private val locks: MemberIdentityLockService,
    private val accounts: MemberAccountPersistencePort,
    private val oauth: MemberOAuthPersistencePort,
    private val credentials: MemberCredentialPersistencePort,
    private val identities: LoginIdentityValidator,
) {
    @Transactional(readOnly = true)
    fun get(memberId: Long): MemberAccountResponse {
        val social = accounts.findOAuths(memberId)
        val email = credentials.findByMemberId(MemberId(memberId))?.takeIf { it.password.isNotBlank() }
        val canUnlink = social.size + (if (email == null) 0 else 1) > 1
        return MemberAccountResponse(
            social.map {
                ConnectedMemberAccount(requireNotNull(it.id).value, LoginMethod.from(it.provider), it.email, canUnlink)
            } +
                listOfNotNull(
                    email?.let {
                        ConnectedMemberAccount(requireNotNull(it.id).value, LoginMethod.EMAIL, it.email, false)
                    },
                ),
        )
    }

    @Transactional
    fun link(
        memberId: Long,
        identity: LoginIdentity?,
        proof: OAuthAttributes,
    ): MemberAccountResponse {
        lockAndValidate(memberId, identity)
        val existing = oauth.findByProviderAndExternalId(proof.getProvider(), proof.getExternalId())
        if (existing != null && existing.memberId.value != memberId) throw MemberOAuthConflictException()
        if (existing == null) {
            val member = members.findById(MemberId(memberId)) ?: throw MemberNotFoundException()
            try {
                oauth.save(
                    MemberOAuth.of(proof.getExternalId(), proof.getProvider(), MemberId(memberId), proof.getEmail()),
                    member,
                )
            } catch (_: org.springframework.dao.DataIntegrityViolationException) {
                throw MemberOAuthConflictException()
            }
        }
        return get(memberId)
    }

    @Transactional
    fun unlink(
        memberId: Long,
        identity: LoginIdentity?,
        oauthId: Long,
    ): MemberAccountResponse {
        lockAndValidate(memberId, identity)
        val social = accounts.findOAuths(memberId)
        if (social.none { it.id?.value == oauthId }) return get(memberId)
        val email = credentials.findByMemberId(MemberId(memberId))?.takeIf { it.password.isNotBlank() }
        if (social.size + (if (email == null) 0 else 1) <= 1) {
            throw MemberAccountException(
                MemberExceptionCode.MEMBER_LAST_LOGIN_ACCOUNT,
            )
        }
        // Deleted OAuth IDs are never reused: access and refresh validation reject this identity immediately.
        accounts.deleteOAuth(memberId, oauthId)
        return get(memberId)
    }

    private fun lockAndValidate(
        memberId: Long,
        identity: LoginIdentity?,
    ) {
        locks.lockMember(MemberId(memberId))
        if (!identities.isValid(
                memberId,
                identity,
            )
        ) {
            throw MemberAccountException(MemberExceptionCode.MEMBER_ACCOUNT_REAUTH_REQUIRED)
        }
    }
}

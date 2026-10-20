package core.application.member.application.service.auth

import core.application.authorization.application.service.RoleQueryService
import core.application.member.application.exception.InvalidEmailPasswordException
import core.application.member.application.exception.MemberAllowedException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.service.MemberIdentityLockService
import core.application.member.application.service.TrackMemberBadges
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.refreshToken.application.service.RefreshTokenIssueService
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.aggregate.Member
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.membercredential.aggregate.MemberCredential
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 이메일 비밀번호 인증을 위한 별도 서비스입니다.
 *
 * Member 도메인과는 분리되어 있어, 추후 비밀번호 로그인 기능을 제거할 때
 * 이 서비스와 관련 코드만 삭제하면 됩니다.
 *
 * login 함수는 이제 회원가입(Signup)도 지원합니다.
 */
@Service
class EmailPasswordAuthService(
    private val memberCredentialPersistencePort: MemberCredentialPersistencePort,
    private val memberPersistencePort: MemberPersistencePort,
    private val identityLock: MemberIdentityLockService,
    private val roleQueryService: RoleQueryService,
    private val memberRoleService: MemberRoleService,
    private val memberTeamService: MemberTeamService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenIssueService: RefreshTokenIssueService,
    private val passwordEncoder: PasswordEncoder,
) {
    /**
     * 이메일과 비밀번호로 로그인하거나, 신규 회원가입을 처리합니다.
     *
     * MemberCredential이 없는 경우 신규 회원을 생성합니다.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @TrackMemberBadges
    fun login(
        email: String,
        password: String,
        deviceId: String? = null,
    ): AuthTokenResponse {
        // 1. Find credential by email
        val credential = memberCredentialPersistencePort.findByEmail(email)

        val (member, loginCredential) =
            if (credential == null) {
                if (memberPersistencePort.findAllBySignupEmail(email).isNotEmpty()) {
                    throw InvalidEmailPasswordException()
                }
                return signupNewMember(email, password, deviceId)
            } else {
                // 2. Verify password
                if (!passwordEncoder.matches(password, credential.password)) {
                    throw InvalidEmailPasswordException()
                }

                // 3. Find member and validate status (re-link if needed)
                val existingMember = memberPersistencePort.findById(credential.memberId)
                if (existingMember != null) {
                    existingMember to credential
                } else {
                    throw MemberNotFoundException()
                }
            }

        validateMemberForLogin(member)

        memberRoleService.ensureGuestRoleAssigned(member.id!!)
        memberTeamService.ensureMemberTeamInitialized(member.id!!)

        // Generate JWT tokens
        val loginIdentity = LoginIdentity(LoginMethod.EMAIL, loginCredential.id!!.value)
        val permissionStrings = roleQueryService.getPermissionsByMemberId(member.id!!)
        val authorities =
            permissionStrings.map {
                org.springframework.security.core.authority.SimpleGrantedAuthority(
                    it,
                )
            }

        val accessToken =
            jwtTokenProvider.generateAccessTokenWithPermissions(
                member.id!!.toString(),
                authorities,
                loginIdentity,
            )

        val issued = refreshTokenIssueService.issueForLogin(member.id!!, deviceId, loginIdentity)

        return AuthTokenResponse(accessToken, issued.requirePlainToken())
    }

    /**
     * 신규 회원을 생성하고 자격 증명을 저장합니다.
     */
    private fun signupNewMember(
        email: String,
        password: String,
        deviceId: String?,
    ): AuthTokenResponse {
        // 1. Create new member
        val memberName = email.substringBefore("@")

        val newMember =
            memberPersistencePort.save(
                Member.createPending(
                    email = email,
                    name = memberName,
                ),
            )

        // 2. Ensure default member role (GUEST)
        memberRoleService.ensureGuestRoleAssigned(newMember.id!!)
        memberTeamService.ensureMemberTeamInitialized(newMember.id!!)

        // 3. Create MemberCredential with encoded password
        val encodedPassword = passwordEncoder.encode(password)
        val newCredential =
            memberCredentialPersistencePort.save(
                MemberCredential.create(
                    memberId = newMember.id!!,
                    email = email,
                    encodedPassword = encodedPassword,
                ),
            )
        val loginIdentity = LoginIdentity(LoginMethod.EMAIL, newCredential.id!!.value)

        // 4. Generate JWT tokens
        val permissionStrings = roleQueryService.getPermissionsByMemberId(newMember.id!!)
        val authorities =
            permissionStrings.map {
                org.springframework.security.core.authority.SimpleGrantedAuthority(
                    it,
                )
            }

        val accessToken =
            jwtTokenProvider.generateAccessTokenWithPermissions(
                newMember.id!!.toString(),
                authorities,
                loginIdentity,
            )

        val issued = refreshTokenIssueService.issueForLogin(newMember.id!!, deviceId, loginIdentity)

        return AuthTokenResponse(accessToken, issued.requirePlainToken())
    }

    private fun validateMemberForLogin(member: Member) {
        identityLock.lockMember(requireNotNull(member.id))
        if (!member.isAllowed() && member.status !in setOf(MemberStatus.PENDING, MemberStatus.REJECTED)) {
            throw MemberAllowedException()
        }

        if (memberPersistencePort.existsDeletedMemberById(member.id!!.value)) {
            throw MemberDeletedException()
        }
    }

    /**
     * 회원에게 비밀번호를 설정합니다.
     */
    @Transactional
    fun setPassword(
        memberId: MemberId,
        oldPassword: String?,
        newPassword: String,
    ) {
        identityLock.lockMember(memberId)
        val existingCredential = memberCredentialPersistencePort.findByMemberId(memberId)

        // 이메일 추가 연결은 별도 본인 확인을 제공하기 전까지 허용하지 않는다.
        if (existingCredential == null || oldPassword == null || !passwordEncoder.matches(oldPassword, existingCredential.password)) {
            throw InvalidEmailPasswordException()
        }
        memberCredentialPersistencePort.updatePassword(
            existingCredential.id!!,
            passwordEncoder.encode(newPassword),
            Instant.now(),
        )
    }

    /**
     * 이메일 로그인 해제는 별도 정책 확정 전까지 제공하지 않습니다.
     */
    @Transactional
    fun removePassword(memberId: MemberId) {
        identityLock.lockMember(memberId)
        throw InvalidEmailPasswordException()
    }

    /**
     * 회원의 비밀번호 자격 증명 존재 여부를 확인합니다.
     */
    fun hasPassword(memberId: MemberId): Boolean {
        return memberCredentialPersistencePort.findByMemberId(memberId) != null
    }
}

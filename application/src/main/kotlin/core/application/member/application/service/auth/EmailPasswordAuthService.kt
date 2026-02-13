package core.application.member.application.service.auth

import core.application.common.constant.Profile
import core.application.member.application.exception.InvalidEmailPasswordException
import core.application.member.application.exception.MemberAllowedException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.service.role.MemberRoleService
import core.application.authorization.application.service.RoleQueryService
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.membercredential.aggregate.MemberCredential
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.core.env.Environment
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
    private val roleQueryService: RoleQueryService,
    private val memberRoleService: MemberRoleService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
    private val passwordEncoder: PasswordEncoder,
    private val environment: Environment,
) {
    /**
     * 이메일과 비밀번호로 로그인하거나, 신규 회원가입을 처리합니다.
     *
     * MemberCredential이 없는 경우 신규 회원을 생성합니다.
     */
    @Transactional
    fun login(
        email: String,
        password: String,
    ): AuthTokenResponse {
        // 1. Find credential by email
        val credential = memberCredentialPersistencePort.findByEmail(email)

        if (credential == null) {
            // 신규 회원 가입 (Signup)
            return signupNewMember(email, password)
        }

        // 2. Verify password
        if (!passwordEncoder.matches(password, credential.password)) {
            throw InvalidEmailPasswordException()
        }

        // 3. Find member and validate status
        val member =
            memberPersistencePort.findById(credential.memberId)
                ?: throw MemberNotFoundException()

        // 4. Check if member is allowed to login
        if (!member.isAllowed()) {
            throw MemberAllowedException()
        }

        // 5. Check if deleted
        if (memberPersistencePort.existsDeletedMemberById(credential.memberId.value)) {
            throw MemberDeletedException()
        }

        // 6. Generate JWT tokens
        val permissionStrings = roleQueryService.getPermissionsByMemberId(member.id!!)
        val authorities = permissionStrings.map { org.springframework.security.core.authority.SimpleGrantedAuthority(it) }

        val accessToken =
            jwtTokenProvider.generateAccessTokenWithPermissions(
                member.id!!.toString(),
                authorities,
            )

        val refreshToken = jwtTokenProvider.generateRefreshToken(member.id!!.toString())

        // 7. Save refresh token
        val refreshTokenEntity = RefreshToken.create(member.id!!, refreshToken)
        refreshTokenPersistencePort.save(refreshTokenEntity)

        return AuthTokenResponse(accessToken, refreshToken)
    }

    /**
     * 신규 회원을 생성하고 자격 증명을 저장합니다.
     */
    private fun signupNewMember(
        email: String,
        password: String,
    ): AuthTokenResponse {
        // 1. Create new member
        val activeProfile = Profile.get(environment).value
        val memberName = email.substringBefore("@")

        val newMember = memberPersistencePort.save(
            Member.create(
                email = email,
                name = memberName,
                activeProfile = activeProfile,
            )
        )

        // 2. Create default member role (GUEST)
        memberRoleService.assignGuestRole(newMember.id!!)

        // 3. Create MemberCredential with encoded password
        val encodedPassword = passwordEncoder.encode(password)
        val newCredential =
            MemberCredential.create(
                memberId = newMember.id!!,
                email = email,
                encodedPassword = encodedPassword,
            )
        memberCredentialPersistencePort.save(newCredential)

        // 4. Generate JWT tokens
        val permissionStrings = roleQueryService.getPermissionsByMemberId(newMember.id!!)
        val authorities = permissionStrings.map { org.springframework.security.core.authority.SimpleGrantedAuthority(it) }

        val accessToken =
            jwtTokenProvider.generateAccessTokenWithPermissions(
                newMember.id!!.toString(),
                authorities,
            )

        val refreshToken = jwtTokenProvider.generateRefreshToken(newMember.id!!.toString())

        // 5. Save refresh token
        val refreshTokenEntity = RefreshToken.create(newMember.id!!, refreshToken)
        refreshTokenPersistencePort.save(refreshTokenEntity)

        return AuthTokenResponse(accessToken, refreshToken)
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
        val existingCredential = memberCredentialPersistencePort.findByMemberId(memberId)

        // Verify old password if provided and credential exists
        if (oldPassword != null && existingCredential != null) {
            if (!passwordEncoder.matches(oldPassword, existingCredential.password)) {
                throw InvalidEmailPasswordException()
            }
        }

        val encodedPassword = passwordEncoder.encode(newPassword)

        if (existingCredential != null) {
            // Update existing credential
            memberCredentialPersistencePort.updatePassword(
                existingCredential.id!!,
                encodedPassword,
                Instant.now(),
            )
        } else {
            // Find member to get email
            val member =
                memberPersistencePort.findById(memberId)
                    ?: throw MemberNotFoundException()

            // Create new credential
            val newCredential =
                MemberCredential.create(
                    memberId = memberId,
                    email = member.signupEmail,
                    encodedPassword = encodedPassword,
                )
            memberCredentialPersistencePort.save(newCredential)
        }
    }

    /**
     * 회원의 비밀번호 자격 증명을 삭제합니다 (비밀번호 로그인 해제).
     */
    @Transactional
    fun removePassword(memberId: MemberId) {
        memberCredentialPersistencePort.deleteByMemberId(memberId)
    }

    /**
     * 회원의 비밀번호 자격 증명 존재 여부를 확인합니다.
     */
    fun hasPassword(memberId: MemberId): Boolean {
        return memberCredentialPersistencePort.findByMemberId(memberId) != null
    }
}

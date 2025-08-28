package com.server.dpmcore.member.member.application

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.member.member.application.exception.MemberNotFoundException
import com.server.dpmcore.member.member.domain.exception.MemberTeamNotFoundException
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.model.query.MemberNameAuthorityQueryModel
import com.server.dpmcore.member.member.domain.port.inbound.MemberQueryByAuthorityUseCase
import com.server.dpmcore.member.member.domain.port.inbound.MemberQueryUseCase
import com.server.dpmcore.member.member.domain.port.outbound.MemberPersistencePort
import com.server.dpmcore.member.member.presentation.response.MemberDetailsResponse
import com.server.dpmcore.member.memberAuthority.application.MemberAuthorityService
import com.server.dpmcore.refreshToken.domain.port.inbound.RefreshTokenInvalidator
import com.server.dpmcore.security.oauth.token.JwtTokenInjector
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REFRESH_TOKEN
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberQueryService(
    private val memberPersistencePort: MemberPersistencePort,
    private val memberAuthorityService: MemberAuthorityService,
    private val tokenInjector: JwtTokenInjector,
    private val refreshTokenInvalidator: RefreshTokenInvalidator,
) : MemberQueryByAuthorityUseCase,
    MemberQueryUseCase {
    /**
     * 멤버의 식별자를 기반으로 이메일, 이름, 파트, 기수, 관리자 여부를 포함한 기본 프로필 정보를 조회함.
     *
     * @throws MemberNotFoundException
     *
     * @author LeeHanEum
     * @since 2025.07.17
     */
    fun memberMe(memberId: MemberId): MemberDetailsResponse =
        MemberDetailsResponse.of(
            getMemberById(memberId),
            memberAuthorityService.getAuthorityNamesByMemberId(memberId),
        )

    /**
     * 멤버의 식별자를 기반으로 멤버 객체를 조회함.
     *
     * @throws MemberNotFoundException
     *
     * @author LeeHanEum
     * @since 2025.07.17
     */
    fun getMemberById(memberId: MemberId) =
        memberPersistencePort.findById(memberId.value)
            ?: throw MemberNotFoundException()

    /**
     * 멤버를 탈퇴 처리(Soft Delete)하고, 클라이언트의 Refresh Token을 무효화 및 삭제함.
     *
     * @throws MemberNotFoundException
     *
     * @author LeeHanEum
     * @since 2025.07.17
     */
    @Transactional
    fun withdraw(
        memberId: MemberId,
        response: HttpServletResponse,
    ) {
        if (!memberPersistencePort.existsById(memberId.value)) throw MemberNotFoundException()

        tokenInjector.invalidateCookie(REFRESH_TOKEN, response)
        refreshTokenInvalidator.destroyRefreshToken(memberId)

        memberPersistencePort.delete(memberId.value)
    }

    /**
     * 기수에 속한 멤버들의 식별자를 목록 조회함.
     *
     * @author its-sky
     * @since 2025.07.23
     */
    fun getMembersByCohort(value: String): List<MemberId> =
        memberPersistencePort
            .findAllByCohort(value)

    /**
     * 멤버의 식별자를 기반으로 해당 멤버의 팀 번호를 조회함.
     *
     * @throws MemberTeamNotFoundException
     *
     * @author its-sky
     * @since 2025.07.27
     */
    fun getMemberTeamNumber(memberId: MemberId): Int =
        memberPersistencePort.findMemberTeamByMemberId(memberId)
            ?: throw MemberTeamNotFoundException()

    override fun findAllMemberIdByAuthorityIds(authorityIds: List<AuthorityId>): List<MemberId> =
        memberPersistencePort
            .findAllMemberIdByAuthorityIds(authorityIds)

    override fun getMemberNameAuthorityByMemberId(memberId: MemberId): List<MemberNameAuthorityQueryModel> =
        memberPersistencePort.findMemberNameAndAuthorityByMemberId(memberId)
}

package com.server.dpmcore.member.member.application

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.member.member.application.exception.MemberNotFoundException
import com.server.dpmcore.member.member.domain.exception.MemberTeamNotFoundException
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.port.inbound.MemberQueryByAuthorityUseCase
import com.server.dpmcore.member.member.domain.port.inbound.MemberQueryUseCase
import com.server.dpmcore.member.member.domain.port.inbound.query.MemberNameAuthorityQueryModel
import com.server.dpmcore.member.member.domain.port.outbound.MemberPersistencePort
import com.server.dpmcore.member.member.presentation.response.MemberDetailsResponse
import com.server.dpmcore.refreshToken.domain.port.inbound.RefreshTokenInvalidator
import com.server.dpmcore.security.oauth.token.JwtTokenInjector
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REFRESH_TOKEN
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberQueryService(
    private val memberPersistencePort: MemberPersistencePort,
    private val tokenInjector: JwtTokenInjector,
    private val refreshTokenInvalidator: RefreshTokenInvalidator,
) : MemberQueryByAuthorityUseCase,
    MemberQueryUseCase {
    fun memberMe(memberId: MemberId): MemberDetailsResponse =
        MemberDetailsResponse.from(
            getMemberById(memberId),
        )

    fun getMemberById(memberId: MemberId) =
        memberPersistencePort.findById(memberId.value)
            ?: throw MemberNotFoundException()

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

    override fun findAllMemberIdByAuthorityIds(authorityIds: List<AuthorityId>): List<MemberId> =
        memberPersistencePort
            .findAllMemberIdByAuthorityIds(authorityIds)

    fun getMembersByCohort(value: String): List<MemberId> =
        memberPersistencePort
            .findAllByCohort(value)

    override fun getMemberNameAuthorityByMemberId(memberId: MemberId): MemberNameAuthorityQueryModel =
        memberPersistencePort.findMemberNameAndAuthorityByMemberId(memberId)

    fun getMemberTeamNumber(memberId: MemberId): Int =
        memberPersistencePort.findMemberTeamByMemberId(memberId)
            ?: throw MemberTeamNotFoundException()
}

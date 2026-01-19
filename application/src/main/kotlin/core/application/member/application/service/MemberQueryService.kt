package core.application.member.application.service

import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberTeamNotFoundException
import core.application.member.application.service.role.MemberRoleService
import core.application.member.presentation.response.MemberDetailsResponse
import core.domain.authorization.vo.RoleId
import core.domain.member.aggregate.Member
import core.domain.member.port.inbound.MemberQueryByRoleUseCase
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberNameRoleQueryModel
import core.domain.member.vo.MemberId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MemberQueryService(
    private val memberPersistencePort: MemberPersistencePort,
    private val memberRoleService: MemberRoleService,
    @Value("\${member.default-team-id:8}")
    private val defaultTeamId: Int,
) : MemberQueryByRoleUseCase,
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
            memberRoleService.getRoleNamesByMemberId(memberId),
            getMemberTeamNumber(memberId),
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
            ?: defaultTeamId

    fun checkWhiteList(
        name: String,
        signupEmail: String,
    ): Member =
        memberPersistencePort.findByNameAndSignupEmail(name, signupEmail)
            ?: throw MemberNotFoundException()

    override fun getMembersByIds(memberIds: List<MemberId>) = memberPersistencePort.findAllByIds(memberIds)

    override fun findAllMemberIdByRoleIds(roleIds: List<RoleId>): List<MemberId> =
        memberPersistencePort
            .findAllMemberIdByRoleIds(roleIds)

    override fun getMemberNameRoleByMemberId(memberId: MemberId): List<MemberNameRoleQueryModel> =
        memberPersistencePort.findMemberNameAndRoleByMemberId(memberId)
}

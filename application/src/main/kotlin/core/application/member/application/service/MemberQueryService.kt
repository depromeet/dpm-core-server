package core.application.member.application.service

import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.exception.MemberTeamNotFoundException
import core.application.member.application.service.access.MemberAccessService
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.member.presentation.response.AppleHiddenEmailMemberResponse
import core.application.member.presentation.response.AppleHiddenEmailMembersResponse
import core.application.member.presentation.response.MemberDetailsResponse
import core.domain.authorization.vo.RoleId
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.inbound.MemberQueryByRoleUseCase
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberNameRoleQueryModel
import core.domain.member.port.outbound.query.MemberOverviewQueryModel
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamId
import core.domain.team.vo.TeamNumber
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MemberQueryService(
    private val memberPersistencePort: MemberPersistencePort,
    private val memberAccessService: MemberAccessService,
    private val memberOAuthService: MemberOAuthService,
    private val cohortQueryUseCase: CohortQueryUseCase,
    @Value("\${member.default-team-id:0}")
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
            memberAccessService.isAdmin(memberId),
            getMemberTeamNumber(memberId),
        )

    /**
     * 멤버의 식별자를 기반으로 멤버 객체를 조회함.
     *
     * @throws MemberNotFoundException
     *
     * @author LeeHanEum
     * @since 2025.07.17
     * @update 2026.01.16 junwon service에서 usecase로 이동
     */
    override fun getMemberById(memberId: MemberId) =
        memberPersistencePort.findById(memberId)
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

    override fun getMemberIdsByCohortId(cohortId: CohortId): List<MemberId> =
        memberPersistencePort.findAllByCohortId(
            cohortId,
        )

    /**
     * 멤버의 식별자를 기반으로 해당 멤버의 팀 번호를 조회함.
     *
     * @throws MemberTeamNotFoundException
     *
     * @author its-sky
     * @since 2025.07.27
     */
    override fun getMemberTeamNumber(memberId: MemberId): TeamNumber =
        TeamNumber(
            memberPersistencePort.findMemberTeamNumberByMemberId(memberId)
                ?: defaultTeamId,
        )

    override fun getMemberTeamNumberByMemberIds(memberIds: List<MemberId>): Map<MemberId, TeamNumber> =
        memberPersistencePort.findMemberTeamNumberByMemberIds(memberIds)
            .mapKeys { MemberId(it.key) }
            .mapValues { TeamNumber(it.value) }

    override fun getMemberTeamId(memberId: MemberId): TeamId =
        TeamId(
            memberPersistencePort.findMemberTeamIdByMemberId(memberId)
                ?: defaultTeamId.toLong(),
        )

    fun checkWhiteList(email: String): Member? = memberPersistencePort.findBySignupEmail(email)

    fun getAppleHiddenEmailMembers(): AppleHiddenEmailMembersResponse =
        AppleHiddenEmailMembersResponse(
            members =
                memberOAuthService
                    .findMemberIdsByProvider(OAuthProvider.APPLE)
                    .mapNotNull { memberPersistencePort.findById(it) }
                    .asSequence()
                    .filter { it.deletedAt == null }
                    .filter { isHashLikeName(it.name) }
                    .sortedWith(
                        compareBy<Member> { it.createdAt ?: Instant.EPOCH }
                            .thenBy { it.id?.value ?: Long.MAX_VALUE },
                    )
                    .map {
                        AppleHiddenEmailMemberResponse(
                            memberId = requireNotNull(it.id).value,
                            name = it.name,
                            part = it.part?.name,
                            email = it.email ?: it.signupEmail,
                        )
                    }
                    .toList(),
        )

    fun getMembersOverview(latest: Boolean?): List<MemberOverviewQueryModel> =
        memberPersistencePort.findAllOrderedByHighestCohortAndStatus(
            latest = latest,
            latestCohortId = cohortQueryUseCase.getLatestCohortId().value,
        )

    fun getMembersForWhitelist(memberIds: List<Long>): List<Member> =
        memberIds
            .distinct()
            .map { memberId ->
                getMemberById(MemberId(memberId)).also { member ->
                    if (member.deletedAt != null) {
                        throw MemberDeletedException()
                    }
                }
            }

    override fun getMembersByIds(memberIds: List<MemberId>) = memberPersistencePort.findAllByIds(memberIds)

    override fun findAllMemberIdByRoleIds(roleIds: List<RoleId>): List<MemberId> =
        memberPersistencePort
            .findAllMemberIdByRoleIds(roleIds)

    override fun getMemberNameRoleByMemberId(memberId: MemberId): List<MemberNameRoleQueryModel> =
        memberPersistencePort.findMemberNameAndRoleByMemberId(memberId)

    override fun getAll(): List<Member> = memberPersistencePort.findAll()

    override fun findAllMemberIdsByCohortIdAndAuthorityId(
        cohortId: CohortId,
        authorityId: AuthorityId,
    ): List<MemberId> =
        memberPersistencePort.findAllMemberIdsByCohortIdAndAuthorityId(
            cohortId,
            authorityId,
        ).distinct()

    private fun isHashLikeName(name: String): Boolean {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return false
        }

        return UUID_REGEX.matches(normalizedName) || HASH_LIKE_REGEX.matches(normalizedName)
    }

    private companion object {
        private val UUID_REGEX =
            Regex(
                pattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            )
        private val HASH_LIKE_REGEX = Regex("^[0-9a-fA-F]{16,64}$")
    }
}

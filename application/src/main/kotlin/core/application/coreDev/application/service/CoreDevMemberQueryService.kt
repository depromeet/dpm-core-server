package core.application.coreDev.application.service

import core.application.cohort.application.exception.CohortNotFoundException
import core.application.coreDev.presentation.response.CohortInfoDetailResponse
import core.application.coreDev.presentation.response.CoreDevMemberDetailResponse
import core.application.coreDev.presentation.response.CoreDevMemberListResponse
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.authority.MemberAuthorityService
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import core.domain.coreDev.port.inbound.CoreDevMemberQueryUseCase
import core.domain.member.aggregate.Member
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class CoreDevMemberQueryService(
    val memberQueryService: MemberQueryService,
    val memberAuthorityService: MemberAuthorityService,
    private val memberPersistencePort: MemberPersistencePort,
    private val cohortPersistencePort: CohortPersistencePort,
    @Value("\${member.default-team-id:0}")
    private val defaultTeamId: Int,
) : CoreDevMemberQueryUseCase {
    fun getAllMembers(): CoreDevMemberListResponse {
        val allMembers: List<Member> = memberQueryService.getAll()

        return CoreDevMemberListResponse.from(
            allMembers.map { member ->
                val memberId: MemberId = member.id ?: throw MemberNotFoundException()
                val cohortMap: Map<CohortId, Cohort> =
                    cohortPersistencePort.findAll().associateBy {
                        it.id ?: throw CohortNotFoundException()
                    }
                val authorities: List<String> = memberAuthorityService.getAuthorityNamesByMemberId(memberId)
                val teamNumber: Int = getMemberTeamNumber(memberId)

                val cohortInfos: List<CohortInfoDetailResponse> =
                    member.memberCohorts.mapNotNull { memberCohort ->
                        val cohort = cohortMap[memberCohort.cohortId]
                        cohort?.let {
                            CohortInfoDetailResponse.of(
                                cohort = it.value,
                                teamNumber = teamNumber,
                                authorities = authorities,
                            )
                        }
                    }

                CoreDevMemberDetailResponse.of(
                    member = member,
                    cohortInfos = cohortInfos,
                )
            },
        )
    }

    fun getMemberTeamNumber(memberId: MemberId): Int =
        memberPersistencePort.findMemberTeamByMemberId(memberId)
            ?: defaultTeamId
}

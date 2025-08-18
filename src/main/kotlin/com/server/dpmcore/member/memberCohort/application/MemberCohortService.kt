package com.server.dpmcore.member.memberCohort.application

import com.server.dpmcore.cohort.application.CohortQueryService
import com.server.dpmcore.cohort.domain.exception.CohortNotFoundException
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.memberCohort.domain.model.MemberCohort
import com.server.dpmcore.member.memberCohort.domain.port.outbound.MemberCohortPersistencePort
import org.springframework.stereotype.Service

@Service
class MemberCohortService(
    private val memberCohortPersistencePort: MemberCohortPersistencePort,
    private val cohortQueryService: CohortQueryService,
) {
    /**
     * 가장 최신 기수 정보를 조회하고 멤버를 해당 기수에 추가함.
     *
     * @throws CohortNotFoundException
     *
     * @author LeeHanEum
     * @since 2025.08.02
     */
    fun addMemberToCohort(memberId: MemberId) {
        memberCohortPersistencePort.save(
            MemberCohort.of(memberId, cohortQueryService.getLatestCohortId()),
        )
    }
}

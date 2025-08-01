package com.server.dpmcore.member.memberCohort.application

import com.server.dpmcore.cohort.application.CohortQueryService
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.memberCohort.domain.model.MemberCohort
import com.server.dpmcore.member.memberCohort.domain.port.outbound.MemberCohortPersistencePort
import org.springframework.stereotype.Service

@Service
class MemberCohortService(
    private val memberCohortPersistencePort: MemberCohortPersistencePort,
    private val cohortQueryService: CohortQueryService,
) {
    fun addMemberToCohort(memberId: MemberId) {
        memberCohortPersistencePort.save(
            MemberCohort.of(memberId, cohortQueryService.getLatestCohortId()),
        )
    }
}

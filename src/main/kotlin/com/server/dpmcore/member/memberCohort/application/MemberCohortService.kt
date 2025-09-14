package com.server.dpmcore.member.memberCohort.application

import com.server.dpmcore.cohort.application.exception.CohortNotFoundException
import com.server.dpmcore.cohort.domain.port.inbound.CohortQueryUseCase
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.memberCohort.domain.model.MemberCohort
import com.server.dpmcore.member.memberCohort.domain.port.outbound.MemberCohortPersistencePort
import org.springframework.stereotype.Service

@Service
class MemberCohortService(
    private val memberCohortPersistencePort: MemberCohortPersistencePort,
    private val cohortQueryUseCase: CohortQueryUseCase,
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
            MemberCohort.of(memberId, cohortQueryUseCase.getLatestCohortId()),
        )
    }

    /**
     * 멤버 탈퇴 시에, 멤버를 기수에서 제거(Hard Delete)하여 기수 정보 조회 시 노출되지 않도록 함.
     *
     * @author LeeHanEum
     * @since 2025.09.01
     */
    fun deleteMemberFromCohort(memberId: MemberId) {
        memberCohortPersistencePort.deleteByMemberId(memberId.value)
    }
}

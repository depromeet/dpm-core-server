package com.server.dpmcore.member.memberCohort.domain.port.outbound

import com.server.dpmcore.member.memberCohort.domain.model.MemberCohort

interface MemberCohortPersistencePort {
    fun save(memberCohort: MemberCohort)
}

package com.server.dpmcore.member.memberCohort.infrastructure.repository

import com.server.dpmcore.member.memberCohort.infrastructure.entity.MemberCohortEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberCohortJpaRepository : JpaRepository<MemberCohortEntity, Long> {
    fun deleteByMemberId(memberId: Long)
}

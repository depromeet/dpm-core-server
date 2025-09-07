package com.server.dpmcore.member.memberTeam.infrastructure.repository

import com.server.dpmcore.member.memberTeam.infrastructure.entity.MemberTeamEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberTeamJpaRepository : JpaRepository<MemberTeamEntity, Long> {
    fun deleteByMemberId(memberId: Long)
}

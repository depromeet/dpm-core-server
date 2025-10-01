package core.persistence.member.repository.team

import core.entity.member.MemberTeamEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberTeamJpaRepository : JpaRepository<MemberTeamEntity, Long> {
    fun deleteByMemberId(memberId: Long)
}

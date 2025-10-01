package core.persistence.member.repository.cohort

import core.entity.member.MemberCohortEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberCohortJpaRepository : JpaRepository<MemberCohortEntity, Long> {
    fun deleteByMemberId(memberId: Long)
}

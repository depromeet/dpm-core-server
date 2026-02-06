package core.persistence.announcement.repository

import core.domain.announcement.vo.AssignmentId
import core.domain.member.vo.MemberId
import core.entity.announcement.AssignmentSubmissionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AssignmentSubmissionJpaRepository : JpaRepository<AssignmentSubmissionEntity, Long> {
    fun findByAssignmentIdAndMemberId(
        assignmentId: AssignmentId,
        memberId: MemberId,
    ): AssignmentSubmissionEntity?
}

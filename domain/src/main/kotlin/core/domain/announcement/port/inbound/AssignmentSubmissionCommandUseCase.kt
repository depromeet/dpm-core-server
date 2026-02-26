package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.AnnouncementAssignment
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.aggregate.AssignmentSubmission

interface AssignmentSubmissionCommandUseCase {
    fun updateAssignmentSubmission(assignmentSubmission: AssignmentSubmission): AssignmentSubmission

    fun addDeeperInvitationsToSubmission(
        announcementAssignment: AnnouncementAssignment,
        assignment: Assignment,
    )
}

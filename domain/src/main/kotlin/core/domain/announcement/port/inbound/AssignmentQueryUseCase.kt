package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.Assignment

interface AssignmentQueryUseCase {
    fun getAllAssignments(): List<Assignment>
}

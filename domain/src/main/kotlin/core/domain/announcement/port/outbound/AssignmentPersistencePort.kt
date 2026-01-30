package core.domain.announcement.port.outbound

import core.domain.announcement.aggregate.Assignment

interface AssignmentPersistencePort {
    fun save(assignment: Assignment): Assignment
}

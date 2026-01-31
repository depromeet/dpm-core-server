package core.application.announcement.application.service

import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.port.inbound.AssignmentQueryUseCase
import core.domain.announcement.port.outbound.AssignmentPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AssignmentQueryService(
    val assignmentPersistencePort: AssignmentPersistencePort,
) : AssignmentQueryUseCase {
    override fun getAllAssignments(): List<Assignment> = assignmentPersistencePort.findAll()
}

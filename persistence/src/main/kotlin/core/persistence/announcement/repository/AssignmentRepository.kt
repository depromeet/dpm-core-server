package core.persistence.announcement.repository

import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.port.outbound.AssignmentPersistencePort
import core.entity.announcement.AssignmentEntity
import org.springframework.stereotype.Repository

@Repository
class AssignmentRepository(
    val assignmentJpaRepository: AssignmentJpaRepository,
) : AssignmentPersistencePort {
    override fun save(assignment: Assignment): Assignment =
        assignmentJpaRepository.save(AssignmentEntity.from(assignment)).toDomain()

    override fun findAll(): List<Assignment> = assignmentJpaRepository.findAll().map { it.toDomain() }
}

package core.persistence.announcement.repository

import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.port.outbound.AssignmentPersistencePort
import core.domain.announcement.vo.AnnouncementId
import core.domain.announcement.vo.AssignmentId
import core.entity.announcement.AssignmentEntity
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.ANNOUNCEMENT_ASSIGNMENTS
import org.jooq.dsl.tables.references.ASSIGNMENTS
import org.springframework.stereotype.Repository
import java.time.ZoneId

@Repository
class AssignmentRepository(
    val assignmentJpaRepository: AssignmentJpaRepository,
    val dsl: DSLContext,
) : AssignmentPersistencePort {
    override fun save(assignment: Assignment): Assignment =
        assignmentJpaRepository.save(AssignmentEntity.from(assignment)).toDomain()

    override fun findAll(): List<Assignment> = assignmentJpaRepository.findAll().map { it.toDomain() }

    override fun findByAnnouncementId(announcementId: AnnouncementId): Assignment =
        dsl
            .select(
                ASSIGNMENTS.ASSIGNMENT_ID,
                ASSIGNMENTS.SUBMIT_TYPE,
                ASSIGNMENTS.START_AT,
                ASSIGNMENTS.DUE_AT,
                ASSIGNMENTS.SUBMIT_LINK,
                ASSIGNMENTS.CREATED_AT,
                ASSIGNMENTS.UPDATED_AT,
                ASSIGNMENTS.DELETED_AT,
            ).from(ANNOUNCEMENT_ASSIGNMENTS)
            .join(ASSIGNMENTS)
            .on(ANNOUNCEMENT_ASSIGNMENTS.ASSIGNMENT_ID.eq(ASSIGNMENTS.ASSIGNMENT_ID))
            .where(ANNOUNCEMENT_ASSIGNMENTS.ANNOUNCEMENT_ID.eq(announcementId.value))
            .fetchOne { record ->
                Assignment(
                    id = AssignmentId(record[ASSIGNMENTS.ASSIGNMENT_ID]!!),
                    submitType = SubmitType.fromValue(record[ASSIGNMENTS.SUBMIT_TYPE]!!),
                    startAt =
                        record[ASSIGNMENTS.START_AT]!!
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toInstant(),
                    dueAt =
                        record[ASSIGNMENTS.DUE_AT]!!
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toInstant(),
                    submitLink = record[ASSIGNMENTS.SUBMIT_LINK],
                    createdAt =
                        record[ASSIGNMENTS.CREATED_AT]!!
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toInstant(),
                    updatedAt =
                        record[ASSIGNMENTS.UPDATED_AT]!!
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toInstant(),
                    deletedAt =
                        record[ASSIGNMENTS.DELETED_AT]
                            ?.atZone(ZoneId.of("Asia/Seoul"))
                            ?.toInstant(),
                )
            }!!
}

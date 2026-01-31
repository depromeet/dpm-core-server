package core.persistence.announcement.repository

import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.port.outbound.AnnouncementPersistencePort
import core.domain.announcement.port.outbound.query.AnnouncementListItemQueryModel
import core.entity.announcement.AnnouncementEntity
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.ANNOUNCEMENTS
import org.jooq.dsl.tables.references.ANNOUNCEMENT_ASSIGNMENTS
import org.jooq.dsl.tables.references.ANNOUNCEMENT_READS
import org.jooq.dsl.tables.references.ASSIGNMENTS
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class AnnouncementRepository(
    val announcementJpaRepository: AnnouncementJpaRepository,
    val dsl: DSLContext,
) : AnnouncementPersistencePort {
    override fun save(announcement: Announcement): Announcement =
        announcementJpaRepository.save(AnnouncementEntity.from(announcement)).toDomain()

    override fun findAll(): List<Announcement> = announcementJpaRepository.findAll().map { it.toDomain() }

    override fun findAnnouncementListItems(): List<AnnouncementListItemQueryModel> =
        dsl.select(
            ANNOUNCEMENTS.ANNOUNCEMENT_ID,
            ANNOUNCEMENTS.TITLE,
            ANNOUNCEMENTS.ANNOUNCEMENT_TYPE,
            ASSIGNMENTS.SUBMIT_TYPE,
            ANNOUNCEMENTS.CREATED_AT,
            DSL.count(ANNOUNCEMENT_READS.ANNOUNCEMENT_READ_ID.`as`("readMemberCount")),
        )
            .from(ANNOUNCEMENTS)
            .join(ANNOUNCEMENT_ASSIGNMENTS)
            .on(ANNOUNCEMENTS.ANNOUNCEMENT_ID.eq(ANNOUNCEMENT_ASSIGNMENTS.ANNOUNCEMENT_ID))
            .join(ASSIGNMENTS)
            .on(ASSIGNMENTS.ASSIGNMENT_ID.eq(ANNOUNCEMENT_ASSIGNMENTS.ASSIGNMENT_ID))
            .leftJoin(ANNOUNCEMENT_READS)
            .on(ANNOUNCEMENTS.ANNOUNCEMENT_ID.eq(ANNOUNCEMENT_READS.ANNOUNCEMENT_ID))
            .groupBy(
                ANNOUNCEMENTS.ANNOUNCEMENT_ID,
                ANNOUNCEMENTS.TITLE,
                ANNOUNCEMENTS.ANNOUNCEMENT_TYPE,
                ASSIGNMENTS.SUBMIT_TYPE,
                ANNOUNCEMENTS.CREATED_AT,
            )
            .fetchInto(AnnouncementListItemQueryModel::class.java)
}

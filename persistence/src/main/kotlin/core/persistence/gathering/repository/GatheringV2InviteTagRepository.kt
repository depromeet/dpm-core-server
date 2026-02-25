package core.persistence.gathering.repository

import core.domain.cohort.vo.CohortId
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.aggregate.GatheringV2InviteTag
import core.domain.gathering.port.outbound.GatheringV2InviteTagPersistencePort
import core.domain.gathering.vo.GatheringV2Id
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class GatheringV2InviteTagRepository(
    private val jpaRepository: GatheringV2InviteTagJpaRepository,
    private val dsl: DSLContext,
) : GatheringV2InviteTagPersistencePort {
    private val gatheringsV2InviteTags = "gatherings_v2_invite_tags"

    override fun save(
        gatheringV2InviteTag: GatheringV2InviteTag,
        gatheringV2: GatheringV2,
    ) {
        val gatheringId =
            gatheringV2.id?.value
                ?: throw IllegalArgumentException("GatheringV2 id cannot be null")

        val exists =
            jpaRepository.findByGathering_Id(gatheringId).any { existing ->
                existing.tagName == gatheringV2InviteTag.tagName &&
                    existing.cohortId == gatheringV2InviteTag.cohortId.value &&
                    existing.authorityId == gatheringV2InviteTag.authorityId
            }

        if (exists) {
            return
        }

        dsl.insertInto(DSL.table(gatheringsV2InviteTags))
            .set(
                DSL.field("gathering_id"),
                gatheringId,
            ).set(DSL.field("cohort_id"), gatheringV2InviteTag.cohortId.value)
            .set(DSL.field("authority_id"), gatheringV2InviteTag.authorityId)
            .set(DSL.field("tag_name"), gatheringV2InviteTag.tagName)
            .set(DSL.field("created_at"), gatheringV2InviteTag.createdAt ?: java.time.Instant.now())
            .execute()
    }

    override fun findByGatheringId(gatheringId: GatheringV2Id): List<GatheringV2InviteTag> =
        jpaRepository.findByGathering_Id(gatheringId.value).map { it.toDomain() }

    override fun findGatheringIdsByInviteTag(
        cohortId: CohortId?,
        authorityId: Long?,
    ): List<GatheringV2Id> {
        if (cohortId == null && authorityId == null) {
            return emptyList()
        }

        val conditions = mutableListOf<String>()
        val values = mutableListOf<Any>()

        if (cohortId != null) {
            conditions.add("cohort_id = ?")
            values.add(cohortId.value)
        }
        if (authorityId != null) {
            conditions.add("authority_id = ?")
            values.add(authorityId)
        }

        val whereClause = conditions.joinToString(" AND ")

        val records =
            dsl.select(DSL.field("gathering_id", Long::class.java))
                .from(DSL.table(gatheringsV2InviteTags))
                .where(DSL.condition(whereClause, *values.toTypedArray()))
                .fetch()
                .map { it.get("gathering_id", Long::class.java) }

        return records
            .filterNotNull()
            .map { GatheringV2Id(it) }
    }

    override fun findAllDistinct(): List<GatheringV2InviteTag> {
        val records =
            dsl.select(
                DSL.field("cohort_id", Long::class.java),
                DSL.field("authority_id", Long::class.java),
                DSL.field("tag_name", String::class.java),
            )
                .from(DSL.table(gatheringsV2InviteTags))
                .groupBy(
                    DSL.field("cohort_id"),
                    DSL.field("authority_id"),
                    DSL.field("tag_name"),
                )
                .fetch()

        return records.map { record ->
            val placeholderGatheringId = GatheringV2Id(0)
            GatheringV2InviteTag(
                id = null,
                gatheringId = placeholderGatheringId,
                cohortId = CohortId(record.get("cohort_id", Long::class.java) ?: 0),
                authorityId = record.get("authority_id", Long::class.java) ?: 0,
                tagName = record.get("tag_name", String::class.java) ?: "",
                createdAt = null,
            )
        }
    }

    override fun findDistinctByTagName(tagName: String): List<GatheringV2InviteTag> {
        val records =
            dsl.select(
                DSL.field("cohort_id", Long::class.java),
                DSL.field("authority_id", Long::class.java),
                DSL.field("tag_name", String::class.java),
            )
                .from(DSL.table(gatheringsV2InviteTags))
                .where(DSL.field("tag_name").eq(tagName))
                .groupBy(
                    DSL.field("cohort_id"),
                    DSL.field("authority_id"),
                    DSL.field("tag_name"),
                )
                .fetch()

        return records.map { record ->
            val placeholderGatheringId = GatheringV2Id(0)
            GatheringV2InviteTag(
                id = null,
                gatheringId = placeholderGatheringId,
                cohortId = CohortId(record.get("cohort_id", Long::class.java) ?: 0),
                authorityId = record.get("authority_id", Long::class.java) ?: 0,
                tagName = record.get("tag_name", String::class.java) ?: "",
                createdAt = null,
            )
        }
    }

    override fun deleteByGatheringId(gatheringId: GatheringV2Id) {
        jpaRepository.deleteByGathering_Id(gatheringId.value)
    }
}

package core.persistence.afterParty.repository

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.port.outbound.AfterPartyInviteTagPersistencePort
import core.domain.afterParty.vo.AfterPartyId
import core.domain.cohort.vo.CohortId
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class AfterPartyInviteTagRepository(
    private val jpaRepository: AfterPartyInviteTagJpaRepository,
    private val dsl: DSLContext,
) : AfterPartyInviteTagPersistencePort {
    private val afterPartyInviteTags = "after_party_invite_tags"

    override fun save(
        afterPartyInviteTag: AfterPartyInviteTag,
        afterParty: AfterParty,
    ) {
        val afterPartyId =
            afterParty.id?.value
                ?: throw IllegalArgumentException("AfterParty id cannot be null")

        val exists =
            jpaRepository.findByAfterPartyId(afterPartyId).any { existing ->
                existing.tagName == afterPartyInviteTag.tagName &&
                    existing.cohortId == afterPartyInviteTag.cohortId.value &&
                    existing.authorityId == afterPartyInviteTag.authorityId
            }

        if (exists) {
            return
        }

        dsl
            .insertInto(DSL.table(afterPartyInviteTags))
            .set(
                DSL.field("after_party_id"),
                afterPartyId,
            ).set(DSL.field("cohort_id"), afterPartyInviteTag.cohortId.value)
            .set(DSL.field("authority_id"), afterPartyInviteTag.authorityId)
            .set(DSL.field("tag_name"), afterPartyInviteTag.tagName)
            .set(DSL.field("created_at"), afterPartyInviteTag.createdAt ?: Instant.now())
            .execute()
    }

    override fun findByAfterPartyId(afterPartyId: AfterPartyId): List<AfterPartyInviteTag> =
        jpaRepository.findByAfterPartyId(afterPartyId.value).map { it.toDomain() }

    override fun findAfterPartyIdsByInviteTag(
        cohortId: CohortId?,
        authorityId: Long?,
    ): List<AfterPartyId> {
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
            dsl
                .select(DSL.field("after_party_id", Long::class.java))
                .from(DSL.table(afterPartyInviteTags))
                .where(DSL.condition(whereClause, *values.toTypedArray()))
                .fetch()
                .map { it.get("after_party_id", Long::class.java) }

        return records
            .filterNotNull()
            .map { AfterPartyId(it) }
    }

    override fun findAllDistinct(): List<AfterPartyInviteTag> {
        val records =
            dsl
                .select(
                    DSL.field("cohort_id", Long::class.java),
                    DSL.field("authority_id", Long::class.java),
                    DSL.field("tag_name", String::class.java),
                ).from(DSL.table(afterPartyInviteTags))
                .groupBy(
                    DSL.field("cohort_id"),
                    DSL.field("authority_id"),
                    DSL.field("tag_name"),
                ).fetch()

        return records.map { record ->
            val placeholderGatheringId = AfterPartyId(0)
            AfterPartyInviteTag(
                id = null,
                afterPartyId = placeholderGatheringId,
                cohortId = CohortId(record.get("cohort_id", Long::class.java) ?: 0),
                authorityId = record.get("authority_id", Long::class.java) ?: 0,
                tagName = record.get("tag_name", String::class.java) ?: "",
                createdAt = null,
            )
        }
    }

    override fun findDistinctByTagName(tagName: String): List<AfterPartyInviteTag> {
        val records =
            dsl
                .select(
                    DSL.field("cohort_id", Long::class.java),
                    DSL.field("authority_id", Long::class.java),
                    DSL.field("tag_name", String::class.java),
                ).from(DSL.table(afterPartyInviteTags))
                .where(DSL.field("tag_name").eq(tagName))
                .groupBy(
                    DSL.field("cohort_id"),
                    DSL.field("authority_id"),
                    DSL.field("tag_name"),
                ).fetch()

        return records.map { record ->
            val placeholderGatheringId = AfterPartyId(0)
            AfterPartyInviteTag(
                id = null,
                afterPartyId = placeholderGatheringId,
                cohortId = CohortId(record.get("cohort_id", Long::class.java) ?: 0),
                authorityId = record.get("authority_id", Long::class.java) ?: 0,
                tagName = record.get("tag_name", String::class.java) ?: "",
                createdAt = null,
            )
        }
    }

    override fun deleteByAfterPartyId(afterPartyId: AfterPartyId) {
        jpaRepository.deleteByAfterPartyId(afterPartyId.value)
    }
}

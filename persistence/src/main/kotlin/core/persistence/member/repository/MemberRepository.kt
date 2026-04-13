package core.persistence.member.repository

import core.domain.authorization.vo.RoleId
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.constant.AuthorityConstants.ORGANIZER_AUTHORITY_ID
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberNameRoleQueryModel
import core.domain.member.port.outbound.query.MemberOverviewQueryModel
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamNumber
import core.entity.member.MemberEntity
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.AFTER_PARTY
import org.jooq.dsl.tables.references.AFTER_PARTY_INVITEES
import org.jooq.dsl.tables.references.AFTER_PARTY_INVITE_TAGS
import org.jooq.dsl.tables.references.ANNOUNCEMENTS
import org.jooq.dsl.tables.references.ANNOUNCEMENT_ASSIGNMENTS
import org.jooq.dsl.tables.references.ANNOUNCEMENT_READS
import org.jooq.dsl.tables.references.ASSIGNMENTS
import org.jooq.dsl.tables.references.ASSIGNMENT_SUBMISSIONS
import org.jooq.dsl.tables.references.ATTENDANCES
import org.jooq.dsl.tables.references.BILLS
import org.jooq.dsl.tables.references.COHORTS
import org.jooq.dsl.tables.references.GATHERINGS
import org.jooq.dsl.tables.references.GATHERING_MEMBERS
import org.jooq.dsl.tables.references.GATHERING_RECEIPTS
import org.jooq.dsl.tables.references.GATHERING_RECEIPT_PHOTOS
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_COHORTS
import org.jooq.dsl.tables.references.MEMBER_CREDENTIALS
import org.jooq.dsl.tables.references.MEMBER_OAUTH
import org.jooq.dsl.tables.references.MEMBER_PERMISSIONS
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.MEMBER_TEAMS
import org.jooq.dsl.tables.references.NOTIFICATION_TOKENS
import org.jooq.dsl.tables.references.REFRESH_TOKENS
import org.jooq.dsl.tables.references.ROLES
import org.jooq.dsl.tables.references.SENT_ANNOUNCEMENT_NOTIFICATIONS
import org.jooq.dsl.tables.references.TEAMS
import org.jooq.impl.DSL.exists
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.selectOne
import org.jooq.impl.DSL.table
import org.jooq.impl.DSL.`when`
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class MemberRepository(
    private val memberJpaRepository: MemberJpaRepository,
    private val dsl: DSLContext,
) : MemberPersistencePort {
    override fun findBySignupEmail(email: String): Member? = memberJpaRepository.findBySignupEmail(email)?.toDomain()

    override fun findAllBySignupEmail(email: String): List<Member> =
        memberJpaRepository.findAllBySignupEmail(email).map { it.toDomain() }

    override fun save(member: Member): Member =
        if (member.id == null) {
            val now = LocalDateTime.now()
            val inserted =
                dsl
                    .insertInto(MEMBERS)
                    .set(MEMBERS.NAME, member.name)
                    .set(MEMBERS.EMAIL, member.email)
                    .set(MEMBERS.SIGNUP_EMAIL, member.signupEmail)
                    .set(MEMBERS.PART, member.part?.name)
                    .set(MEMBERS.STATUS, member.status.name)
                    .set(MEMBERS.CREATED_AT, now)
                    .set(MEMBERS.UPDATED_AT, now)
                    .returning()
                    .fetchOne()
                    ?: error("Failed to insert member")

            Member(
                id = MemberId(requireNotNull(inserted.memberId)),
                name = requireNotNull(inserted.name),
                email = inserted.email,
                signupEmail = requireNotNull(inserted.signupEmail),
                part = inserted.part?.let(MemberPart::valueOf),
                status = MemberStatus.valueOf(requireNotNull(inserted.status)),
                createdAt = inserted.createdAt?.atZone(ZoneId.of("UTC"))?.toInstant(),
                updatedAt = inserted.updatedAt?.atZone(ZoneId.of("UTC"))?.toInstant(),
                deletedAt = inserted.deletedAt?.atZone(ZoneId.of("UTC"))?.toInstant(),
            )
        } else {
            memberJpaRepository.save(MemberEntity.from(member)).toDomain()
        }

    override fun findById(memberId: MemberId): Member? =
        memberJpaRepository
            .findById(
                memberId.value,
            ).orElse(null)
            ?.toDomain()

    override fun existsById(memberId: Long): Boolean = memberJpaRepository.existsById(memberId)

    override fun findAllByIds(ids: List<MemberId>): List<Member> =
        memberJpaRepository.findAllById(ids.map { it.value }).map { it.toDomain() }

    override fun existsDeletedMemberById(memberId: Long): Boolean =
        memberJpaRepository.existsByIdAndDeletedAtIsNotNull(memberId)

    override fun findByNameAndSignupEmail(
        name: String,
        signupEmail: String,
    ): Member? = memberJpaRepository.findByNameAndSignupEmail(name, signupEmail)?.toDomain()

    override fun findAllMemberIdByRoleIds(roleIds: List<RoleId>): List<MemberId> =
        dsl
            .selectDistinct(MEMBERS.MEMBER_ID)
            .from(MEMBERS)
            .join(MEMBER_ROLES)
            .on(MEMBERS.MEMBER_ID.eq(MEMBER_ROLES.MEMBER_ID))
            .join(ROLES)
            .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .where(ROLES.ROLE_ID.`in`(roleIds.map { it.value }))
            .and(MEMBERS.DELETED_AT.isNull)
            .and(MEMBER_ROLES.DELETED_AT.isNull)
            .fetch(MEMBERS.MEMBER_ID)
            .map { MemberId(it ?: 0L) }

    override fun findAllByCohort(value: String): List<MemberId> =
        dsl
            .select(MEMBERS.MEMBER_ID)
            .from(MEMBERS)
            .join(MEMBER_COHORTS)
            .on(MEMBERS.MEMBER_ID.eq(MEMBER_COHORTS.MEMBER_ID))
            .join(COHORTS)
            .on(MEMBER_COHORTS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .where(COHORTS.VALUE.eq(value))
            .and(MEMBERS.DELETED_AT.isNull)
            .fetch(MEMBERS.MEMBER_ID)
            .filterNotNull()
            .map {
                MemberId(it)
            }

    override fun findAllByCohortId(cohortId: CohortId): List<MemberId> =
        dsl
            .select(MEMBERS.MEMBER_ID)
            .from(MEMBERS)
            .join(MEMBER_COHORTS)
            .on(MEMBERS.MEMBER_ID.eq(MEMBER_COHORTS.MEMBER_ID))
            .where(MEMBER_COHORTS.COHORT_ID.eq(cohortId.value))
            .and(MEMBERS.DELETED_AT.isNull)
            .fetch(MEMBERS.MEMBER_ID)
            .filterNotNull()
            .map { MemberId(it) }

    override fun findAllMemberIdsByCohortIdAndAuthorityId(
        cohortId: CohortId,
        authorityId: AuthorityId,
    ): List<MemberId> =
        run {
            val memberAuthoritiesTable = table(name("member_authorities")).`as`("ma")
            val memberAuthoritiesMemberIdField = field(name("ma", "member_id"), Long::class.java)
            val memberAuthoritiesAuthorityIdField = field(name("ma", "authority_id"), Long::class.java)
            val memberAuthoritiesDeletedAtField = field(name("ma", "deleted_at"), LocalDateTime::class.java)

            dsl
                .selectDistinct(MEMBERS.MEMBER_ID)
                .from(MEMBERS)
                .join(MEMBER_COHORTS)
                .on(MEMBERS.MEMBER_ID.eq(MEMBER_COHORTS.MEMBER_ID))
                .join(memberAuthoritiesTable)
                .on(MEMBERS.MEMBER_ID.eq(memberAuthoritiesMemberIdField))
                .where(MEMBER_COHORTS.COHORT_ID.eq(cohortId.value))
                .and(MEMBERS.DELETED_AT.isNull)
                .and(memberAuthoritiesAuthorityIdField.eq(authorityId.value))
                .and(memberAuthoritiesDeletedAtField.isNull)
                .fetch(MEMBERS.MEMBER_ID)
                .filterNotNull()
                .map { MemberId(it) }
        }

    override fun findMemberNameAndRoleByMemberId(memberId: MemberId): List<MemberNameRoleQueryModel> =
        dsl
            .select(MEMBERS.NAME, ROLES.NAME, MEMBER_ROLES.GRANTED_AT)
            .from(MEMBERS)
            .join(MEMBER_ROLES)
            .on(MEMBERS.MEMBER_ID.eq(MEMBER_ROLES.MEMBER_ID))
            .join(ROLES)
            .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .where(MEMBERS.MEMBER_ID.eq(memberId.value))
            .and(MEMBER_ROLES.DELETED_AT.isNull)
            .fetch()
            .mapNotNull { record ->
                val memberName = record.get(MEMBERS.NAME)
                val roleName = record.get(ROLES.NAME)
                memberName?.let { name ->
                    roleName?.let { role ->
                        MemberNameRoleQueryModel(
                            name = name,
                            role = role,
                            grantedAtEpochMillis =
                                record
                                    .get(MEMBER_ROLES.GRANTED_AT)
                                    ?.atZone(ZoneId.of("Asia/Seoul"))
                                    ?.toInstant()
                                    ?.toEpochMilli(),
                        )
                    }
                }
            }

    override fun findAllOrderedByHighestCohortAndStatus(
        latest: Boolean?,
        latestCohortId: Long,
    ): List<MemberOverviewQueryModel> {
        val cohortValueAsNumber = field("CAST({0} AS UNSIGNED)", Int::class.java, COHORTS.VALUE)
        val maxCohortValue = max(cohortValueAsNumber).`as`("max_cohort_value")
        val maxCohortId = max(MEMBER_COHORTS.COHORT_ID).`as`("max_cohort_id")
        val maxTeamNumber = max(TEAMS.NUMBER).`as`("max_team_number")
        val latestMemberCohorts = MEMBER_COHORTS.`as`("latest_member_cohorts")

        val memberAuthoritiesTable = table(name("member_authorities")).`as`("ma")
        val memberAuthoritiesMemberIdField = field(name("ma", "member_id"), Long::class.java)
        val memberAuthoritiesAuthorityIdField = field(name("ma", "authority_id"), Long::class.java)
        val memberAuthoritiesDeletedAtField = field(name("ma", "deleted_at"), LocalDateTime::class.java)

        val isAdminField =
            exists(
                selectOne()
                    .from(memberAuthoritiesTable)
                    .where(memberAuthoritiesMemberIdField.eq(MEMBERS.MEMBER_ID))
                    .and(memberAuthoritiesAuthorityIdField.eq(ORGANIZER_AUTHORITY_ID))
                    .and(memberAuthoritiesDeletedAtField.isNull),
            ).`as`("is_admin")

        val statusPriority =
            `when`(MEMBERS.STATUS.eq("PENDING"), 0)
                .`when`(MEMBERS.STATUS.eq("ACTIVE"), 1)
                .`when`(MEMBERS.STATUS.eq("INACTIVE"), 2)
                .otherwise(3)

        val hasLatestCohortCondition =
            exists(
                selectOne()
                    .from(latestMemberCohorts)
                    .where(latestMemberCohorts.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                    .and(latestMemberCohorts.COHORT_ID.eq(latestCohortId)),
            )

        val filterCondition =
            when (latest) {
                true -> hasLatestCohortCondition
                false -> hasLatestCohortCondition.not()
                null -> null
            }

        return dsl
            .select(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                MEMBERS.STATUS,
                MEMBERS.PART,
                maxCohortValue,
                maxCohortId,
                maxTeamNumber,
                isAdminField,
            ).from(MEMBERS)
            .leftJoin(MEMBER_COHORTS)
            .on(MEMBER_COHORTS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .leftJoin(COHORTS)
            .on(COHORTS.COHORT_ID.eq(MEMBER_COHORTS.COHORT_ID))
            .leftJoin(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .leftJoin(TEAMS)
            .on(TEAMS.TEAM_ID.eq(MEMBER_TEAMS.TEAM_ID))
            .where(
                MEMBERS.DELETED_AT.isNull
                    .and(filterCondition),
            ).groupBy(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                MEMBERS.STATUS,
                MEMBERS.PART,
            ).orderBy(
                maxCohortValue.desc().nullsLast(),
                statusPriority.asc(),
                MEMBERS.NAME.asc(),
            ).fetch { record ->
                MemberOverviewQueryModel(
                    memberId = requireNotNull(record[MEMBERS.MEMBER_ID]),
                    cohortId = record[maxCohortId],
                    cohortValue = record[maxCohortValue]?.toString(),
                    name = record[MEMBERS.NAME] ?: "",
                    teamNumber = TeamNumber(record[maxTeamNumber] ?: 0),
                    isAdmin = record[isAdminField] ?: false,
                    status = record[MEMBERS.STATUS] ?: "",
                    part = record[MEMBERS.PART],
                )
            }
    }

    override fun findMemberTeamNumberByMemberId(memberId: MemberId): Int? =
        dsl
            .select(TEAMS.NUMBER)
            .from(MEMBER_TEAMS)
            .join(MEMBERS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(MEMBER_TEAMS.MEMBER_ID.eq(memberId.value))
            .orderBy(MEMBER_TEAMS.MEMBER_TEAM_ID.desc())
            .limit(1)
            .fetchOne(TEAMS.NUMBER)

    override fun findMemberTeamIdByMemberId(memberId: MemberId): Long? =
        dsl
            .select(TEAMS.TEAM_ID)
            .from(MEMBER_TEAMS)
            .join(MEMBERS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(MEMBER_TEAMS.MEMBER_ID.eq(memberId.value))
            .orderBy(MEMBER_TEAMS.MEMBER_TEAM_ID.desc())
            .limit(1)
            .fetchOne(TEAMS.TEAM_ID)
            ?.toLong()

    override fun anonymizeIdentity(
        memberId: MemberId,
        email: String,
        signupEmail: String,
    ) {
        dsl
            .update(MEMBERS)
            .set(MEMBERS.EMAIL, email)
            .set(MEMBERS.SIGNUP_EMAIL, signupEmail)
            .set(MEMBERS.UPDATED_AT, LocalDateTime.now())
            .where(MEMBERS.MEMBER_ID.eq(memberId.value))
            .execute()
    }

    override fun hardDeleteById(memberId: MemberId) {
        val value = memberId.value
        val authoredAnnouncementIds =
            dsl
                .select(ANNOUNCEMENTS.ANNOUNCEMENT_ID)
                .from(ANNOUNCEMENTS)
                .where(ANNOUNCEMENTS.AUTHOR_ID.eq(value))
                .fetch(ANNOUNCEMENTS.ANNOUNCEMENT_ID)
                .filterNotNull()
        val authoredAssignmentIds =
            if (authoredAnnouncementIds.isEmpty()) {
                emptyList()
            } else {
                dsl
                    .select(ANNOUNCEMENT_ASSIGNMENTS.ASSIGNMENT_ID)
                    .from(ANNOUNCEMENT_ASSIGNMENTS)
                    .where(ANNOUNCEMENT_ASSIGNMENTS.ANNOUNCEMENT_ID.`in`(authoredAnnouncementIds))
                    .fetch(ANNOUNCEMENT_ASSIGNMENTS.ASSIGNMENT_ID)
                    .filterNotNull()
            }
        val ownedAfterPartyIds =
            dsl
                .select(AFTER_PARTY.AFTER_PARTY_ID)
                .from(AFTER_PARTY)
                .where(AFTER_PARTY.MEMBER_ID.eq(value))
                .fetch(AFTER_PARTY.AFTER_PARTY_ID)
                .filterNotNull()
        val hostedBillIds =
            dsl
                .select(BILLS.BILL_ID)
                .from(BILLS)
                .where(BILLS.HOST_USER_ID.eq(value))
                .fetch(BILLS.BILL_ID)
                .filterNotNull()
        val hostedGatheringIds =
            dsl
                .select(GATHERINGS.GATHERING_ID)
                .from(GATHERINGS)
                .where(GATHERINGS.HOST_USER_ID.eq(value))
                .fetch(GATHERINGS.GATHERING_ID)
                .filterNotNull()
        val billGatheringIds =
            if (hostedBillIds.isEmpty()) {
                emptyList()
            } else {
                dsl
                    .select(GATHERINGS.GATHERING_ID)
                    .from(GATHERINGS)
                    .where(GATHERINGS.BILL_ID.`in`(hostedBillIds))
                    .fetch(GATHERINGS.GATHERING_ID)
                    .filterNotNull()
            }
        val gatheringIdsToDelete = (hostedGatheringIds + billGatheringIds).distinct()
        val receiptIdsToDelete =
            if (gatheringIdsToDelete.isEmpty()) {
                emptyList()
            } else {
                dsl
                    .select(GATHERING_RECEIPTS.RECEIPT_ID)
                    .from(GATHERING_RECEIPTS)
                    .where(GATHERING_RECEIPTS.GATHERING_ID.`in`(gatheringIdsToDelete))
                    .fetch(GATHERING_RECEIPTS.RECEIPT_ID)
                    .filterNotNull()
            }

        if (ownedAfterPartyIds.isNotEmpty()) {
            dsl.deleteFrom(AFTER_PARTY_INVITE_TAGS)
                .where(AFTER_PARTY_INVITE_TAGS.AFTER_PARTY_ID.`in`(ownedAfterPartyIds))
                .execute()
            dsl.deleteFrom(AFTER_PARTY_INVITEES)
                .where(AFTER_PARTY_INVITEES.AFTER_PARTY_ID.`in`(ownedAfterPartyIds))
                .execute()
        }
        dsl.deleteFrom(AFTER_PARTY_INVITEES)
            .where(AFTER_PARTY_INVITEES.MEMBER_ID.eq(value))
            .execute()
        dsl.deleteFrom(AFTER_PARTY)
            .where(AFTER_PARTY.MEMBER_ID.eq(value))
            .execute()

        if (authoredAnnouncementIds.isNotEmpty()) {
            dsl.deleteFrom(SENT_ANNOUNCEMENT_NOTIFICATIONS)
                .where(SENT_ANNOUNCEMENT_NOTIFICATIONS.ANNOUNCEMENT_ID.`in`(authoredAnnouncementIds))
                .execute()
            dsl.deleteFrom(ANNOUNCEMENT_READS)
                .where(ANNOUNCEMENT_READS.ANNOUNCEMENT_ID.`in`(authoredAnnouncementIds))
                .execute()
        }
        dsl.deleteFrom(ANNOUNCEMENT_READS)
            .where(ANNOUNCEMENT_READS.MEMBER_ID.eq(value))
            .execute()
        if (authoredAssignmentIds.isNotEmpty()) {
            dsl.deleteFrom(ASSIGNMENT_SUBMISSIONS)
                .where(ASSIGNMENT_SUBMISSIONS.ASSIGNMENT_ID.`in`(authoredAssignmentIds))
                .execute()
        }
        dsl.deleteFrom(ASSIGNMENT_SUBMISSIONS)
            .where(ASSIGNMENT_SUBMISSIONS.MEMBER_ID.eq(value))
            .execute()
        if (authoredAnnouncementIds.isNotEmpty()) {
            dsl.deleteFrom(ANNOUNCEMENT_ASSIGNMENTS)
                .where(ANNOUNCEMENT_ASSIGNMENTS.ANNOUNCEMENT_ID.`in`(authoredAnnouncementIds))
                .execute()
        }
        if (authoredAssignmentIds.isNotEmpty()) {
            dsl.deleteFrom(ASSIGNMENTS)
                .where(ASSIGNMENTS.ASSIGNMENT_ID.`in`(authoredAssignmentIds))
                .execute()
        }
        dsl.deleteFrom(ANNOUNCEMENTS)
            .where(ANNOUNCEMENTS.AUTHOR_ID.eq(value))
            .execute()

        dsl.deleteFrom(ATTENDANCES)
            .where(ATTENDANCES.MEMBER_ID.eq(value))
            .execute()

        if (gatheringIdsToDelete.isNotEmpty()) {
            dsl.deleteFrom(GATHERING_MEMBERS)
                .where(GATHERING_MEMBERS.GATHERING_ID.`in`(gatheringIdsToDelete))
                .execute()
        }
        dsl.deleteFrom(GATHERING_MEMBERS)
            .where(GATHERING_MEMBERS.MEMBER_ID.eq(value))
            .execute()
        if (receiptIdsToDelete.isNotEmpty()) {
            dsl.deleteFrom(GATHERING_RECEIPT_PHOTOS)
                .where(GATHERING_RECEIPT_PHOTOS.RECEIPT_ID.`in`(receiptIdsToDelete))
                .execute()
        }
        if (gatheringIdsToDelete.isNotEmpty()) {
            dsl.deleteFrom(GATHERING_RECEIPTS)
                .where(GATHERING_RECEIPTS.GATHERING_ID.`in`(gatheringIdsToDelete))
                .execute()
            dsl.deleteFrom(GATHERINGS)
                .where(GATHERINGS.GATHERING_ID.`in`(gatheringIdsToDelete))
                .execute()
        }
        if (hostedBillIds.isNotEmpty()) {
            dsl.deleteFrom(BILLS)
                .where(BILLS.BILL_ID.`in`(hostedBillIds))
                .execute()
        }

        dsl.deleteFrom(NOTIFICATION_TOKENS)
            .where(NOTIFICATION_TOKENS.MEMBER_ID.eq(value))
            .execute()
        dsl.deleteFrom(MEMBER_CREDENTIALS)
            .where(MEMBER_CREDENTIALS.MEMBER_ID.eq(value))
            .execute()
        dsl.deleteFrom(table(name("member_authorities")))
            .where(field(name("member_id"), Long::class.java).eq(value))
            .execute()
        dsl.deleteFrom(MEMBER_PERMISSIONS)
            .where(MEMBER_PERMISSIONS.MEMBER_ID.eq(value))
            .execute()
        dsl.deleteFrom(MEMBER_ROLES)
            .where(MEMBER_ROLES.MEMBER_ID.eq(value))
            .execute()
        dsl.deleteFrom(MEMBER_TEAMS)
            .where(MEMBER_TEAMS.MEMBER_ID.eq(value))
            .execute()
        dsl.deleteFrom(MEMBER_COHORTS)
            .where(MEMBER_COHORTS.MEMBER_ID.eq(value))
            .execute()
        dsl.deleteFrom(MEMBER_OAUTH)
            .where(MEMBER_OAUTH.MEMBER_ID.eq(value))
            .execute()
        dsl.deleteFrom(REFRESH_TOKENS)
            .where(REFRESH_TOKENS.MEMBERID.eq(value))
            .execute()
        dsl.deleteFrom(MEMBERS)
            .where(MEMBERS.MEMBER_ID.eq(value))
            .execute()
    }

    override fun findAll(): List<Member> = memberJpaRepository.findAllByDeletedAtIsNull().map { it.toDomain() }
}

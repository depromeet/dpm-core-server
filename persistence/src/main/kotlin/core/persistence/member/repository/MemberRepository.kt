package core.persistence.member.repository

import core.domain.authorization.vo.RoleId
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberNameRoleQueryModel
import core.domain.member.port.outbound.query.MemberOverviewQueryModel
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamNumber
import core.entity.member.MemberEntity
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.COHORTS
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_COHORTS
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.MEMBER_TEAMS
import org.jooq.dsl.tables.references.ROLES
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
        authorityId: Long,
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
                .and(memberAuthoritiesAuthorityIdField.eq(authorityId))
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

    companion object {
        private const val ORGANIZER_AUTHORITY_ID = 2L
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
            .fetchOne(TEAMS.NUMBER)
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

    override fun findAll(): List<Member> = memberJpaRepository.findAllByDeletedAtIsNull().map { it.toDomain() }
}

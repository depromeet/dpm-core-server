package core.persistence.member.repository

import core.domain.authorization.vo.RoleId
import core.domain.authorization.vo.RoleType
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.port.outbound.query.MemberManagementQueryModel
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
import org.jooq.impl.DSL
import org.jooq.impl.DSL.`when`
import org.jooq.impl.DSL.exists
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.noCondition
import org.jooq.impl.DSL.selectOne
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
class MemberRepository(
    private val memberJpaRepository: MemberJpaRepository,
    private val dsl: DSLContext,
) : MemberPersistencePort {
    override fun softDeleteMembers(memberIds: List<Long>) {
        if (memberIds.isEmpty()) return
        dsl.update(MEMBERS)
            .set(MEMBERS.STATUS, MemberStatus.WITHDRAWN.name)
            .set(MEMBERS.DELETED_AT, LocalDateTime.now(ZoneId.of("UTC")))
            .where(MEMBERS.MEMBER_ID.`in`(memberIds))
            .execute()
    }

    override fun isLoginAvailable(memberId: Long): Boolean =
        dsl.fetchExists(
            dsl.selectOne().from(MEMBERS)
                .where(
                    MEMBERS.MEMBER_ID.eq(memberId),
                    MEMBERS.DELETED_AT.isNull,
                    MEMBERS.STATUS.ne(MemberStatus.WITHDRAWN.name),
                ),
        )

    override fun lockApprovalTargets(memberIds: List<Long>): List<MemberApprovalTarget> {
        if (memberIds.isEmpty()) return emptyList()
        val lockedMembers =
            dsl.select(MEMBERS.MEMBER_ID, MEMBERS.STATUS, MEMBERS.DELETED_AT).from(MEMBERS)
                .where(MEMBERS.MEMBER_ID.`in`(memberIds))
                .orderBy(MEMBERS.MEMBER_ID.asc())
                .forUpdate()
                .fetch()
        val cohortIds =
            dsl.select(MEMBER_COHORTS.MEMBER_ID, MEMBER_COHORTS.COHORT_ID).from(MEMBER_COHORTS)
                .where(MEMBER_COHORTS.MEMBER_ID.`in`(lockedMembers.map { it[MEMBERS.MEMBER_ID] }))
                .forUpdate()
                .fetchGroups(MEMBER_COHORTS.MEMBER_ID, MEMBER_COHORTS.COHORT_ID)
        return lockedMembers.map {
            val memberId = requireNotNull(it[MEMBERS.MEMBER_ID])
            MemberApprovalTarget(
                memberId = memberId,
                status = MemberStatus.valueOf(requireNotNull(it[MEMBERS.STATUS])),
                cohortIds = cohortIds[memberId].orEmpty().filterNotNull().toSet(),
                isDeleted = it[MEMBERS.DELETED_AT] != null,
            )
        }
    }

    override fun findManagementMembers(cohortId: Long): List<MemberManagementQueryModel> {
        val isCurrentCohort =
            exists(
                selectOne().from(MEMBER_COHORTS)
                    .where(MEMBER_COHORTS.MEMBER_ID.eq(MEMBERS.MEMBER_ID), MEMBER_COHORTS.COHORT_ID.eq(cohortId)),
            )
        val hasNoCohort =
            DSL.notExists(
                selectOne().from(MEMBER_COHORTS).where(MEMBER_COHORTS.MEMBER_ID.eq(MEMBERS.MEMBER_ID)),
            )
        val teamNumber =
            field(
                DSL.select(TEAMS.NUMBER).from(MEMBER_TEAMS)
                    .join(TEAMS).on(TEAMS.TEAM_ID.eq(MEMBER_TEAMS.TEAM_ID))
                    .where(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID), TEAMS.COHORT_ID.eq(cohortId))
                    .orderBy(MEMBER_TEAMS.MEMBER_TEAM_ID.desc()).limit(1),
            )
        val hasKakao =
            exists(
                selectOne().from(MEMBER_OAUTH)
                    .where(MEMBER_OAUTH.MEMBER_ID.eq(MEMBERS.MEMBER_ID), MEMBER_OAUTH.PROVIDER.eq("KAKAO")),
            )
        // 같은 제공자의 연결이 여러 개면 가장 최근 ID를 사용한다. 조인하지 않아 회원 행이 늘지 않는다.
        val oauthEmail =
            field(
                DSL.select(MEMBER_OAUTH.EMAIL).from(MEMBER_OAUTH)
                    .where(
                        MEMBER_OAUTH.MEMBER_ID.eq(MEMBERS.MEMBER_ID),
                        MEMBER_OAUTH.PROVIDER.`in`("KAKAO", "APPLE"),
                    )
                    .orderBy(
                        `when`(MEMBER_OAUTH.PROVIDER.eq("KAKAO"), 0).otherwise(1),
                        MEMBER_OAUTH.MEMBER_OAUTH_ID.desc(),
                    ).limit(1),
            )
        return dsl.select(
            MEMBERS.MEMBER_ID, MEMBERS.NAME, MEMBERS.SIGNUP_EMAIL, MEMBERS.PART, MEMBERS.STATUS,
            MEMBERS.CREATED_AT, MEMBERS.UPDATED_AT,
            isCurrentCohort.`as`(
                "is_current_cohort",
            ),
            teamNumber.`as`("team_number"),
            hasKakao.`as`("has_kakao"),
            oauthEmail.`as`("oauth_email"),
        ).from(MEMBERS)
            .where(MEMBERS.DELETED_AT.isNull)
            .and(MEMBERS.STATUS.`in`(MemberStatus.ACTIVE.name, MemberStatus.INACTIVE.name, MemberStatus.PENDING.name))
            .and(isCurrentCohort.or(MEMBERS.STATUS.eq(MemberStatus.PENDING.name).and(hasNoCohort)))
            .fetch { record ->
                val selectedEmail = record.get("oauth_email", String::class.java)?.trim()?.takeIf { it.isNotEmpty() }
                MemberManagementQueryModel(
                    memberId = requireNotNull(record[MEMBERS.MEMBER_ID]),
                    name = requireNotNull(record[MEMBERS.NAME]),
                    email =
                        if (record.get("has_kakao", Boolean::class.java) == true) {
                            selectedEmail
                        } else {
                            selectedEmail ?: requireNotNull(record[MEMBERS.SIGNUP_EMAIL])
                        },
                    part = record[MEMBERS.PART]?.let(MemberPart::valueOf),
                    status = MemberStatus.valueOf(requireNotNull(record[MEMBERS.STATUS])),
                    cohortId = cohortId.takeIf { record.get("is_current_cohort", Boolean::class.java) == true },
                    teamNumber = record.get("team_number", Int::class.java) ?: 0,
                    updatedAt =
                        (record[MEMBERS.UPDATED_AT] ?: record[MEMBERS.CREATED_AT])?.atZone(
                            ZoneId.of("UTC"),
                        )?.toInstant(),
                )
            }
    }

    override fun findBySignupEmail(email: String): Member? = memberJpaRepository.findBySignupEmail(email)?.toDomain()

    override fun lockApprovedManagementMemberIds(
        memberIds: List<Long>,
        cohortId: Long,
    ): List<Long> {
        if (memberIds.isEmpty()) return emptyList()
        return dsl.select(MEMBERS.MEMBER_ID).from(MEMBERS)
            .where(
                MEMBERS.MEMBER_ID.`in`(memberIds),
                MEMBERS.STATUS.`in`("ACTIVE", "INACTIVE"),
                MEMBERS.DELETED_AT.isNull,
                exists(
                    selectOne().from(MEMBER_COHORTS)
                        .where(MEMBER_COHORTS.MEMBER_ID.eq(MEMBERS.MEMBER_ID), MEMBER_COHORTS.COHORT_ID.eq(cohortId)),
                ),
            )
            .orderBy(MEMBERS.MEMBER_ID.asc())
            .forUpdate()
            .fetch(MEMBERS.MEMBER_ID).filterNotNull()
    }

    override fun updateManagementFields(
        memberIds: List<Long>,
        updatePart: Boolean,
        part: MemberPart?,
        status: MemberStatus?,
        changedAssociationMemberIds: Set<Long>,
    ) {
        if (memberIds.isEmpty()) return
        var changed = MEMBERS.MEMBER_ID.`in`(changedAssociationMemberIds)
        if (updatePart) changed = changed.or(MEMBERS.PART.isDistinctFrom(part?.name))
        if (status != null) changed = changed.or(MEMBERS.STATUS.isDistinctFrom(status.name))
        var update = dsl.update(MEMBERS).set(MEMBERS.UPDATED_AT, LocalDateTime.now(ZoneId.of("UTC")))
        if (updatePart) update = update.set(MEMBERS.PART, part?.name)
        if (status != null) update = update.set(MEMBERS.STATUS, status.name)
        update.where(MEMBERS.MEMBER_ID.`in`(memberIds), changed).execute()
    }

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
                profileCompletedAt = inserted.profileCompletedAt?.atZone(ZoneId.of("UTC"))?.toInstant(),
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

    /** 해당 기수 소속 행이 있는 멤버를 모두 반환한다. 여러 기수에 소속된 멤버는 각 기수 명단에 모두 포함된다. */
    override fun findAllByCohort(value: String): List<MemberId> =
        dsl
            .selectDistinct(MEMBERS.MEMBER_ID)
            .from(MEMBERS)
            .join(MEMBER_COHORTS)
            .on(MEMBER_COHORTS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(COHORTS)
            .on(MEMBER_COHORTS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .where(COHORTS.VALUE.eq(value))
            .and(MEMBERS.DELETED_AT.isNull)
            .fetch(MEMBERS.MEMBER_ID)
            .filterNotNull()
            .map { MemberId(it) }

    /** 해당 기수 소속 행이 있는 멤버를 모두 반환한다. 여러 기수에 소속된 멤버는 각 기수 명단에 모두 포함된다. */
    override fun findAllByCohortId(cohortId: CohortId): List<MemberId> =
        dsl
            .selectDistinct(MEMBERS.MEMBER_ID)
            .from(MEMBERS)
            .join(MEMBER_COHORTS)
            .on(MEMBER_COHORTS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .where(MEMBER_COHORTS.COHORT_ID.eq(cohortId.value))
            .and(MEMBERS.DELETED_AT.isNull)
            .fetch(MEMBERS.MEMBER_ID)
            .filterNotNull()
            .map { MemberId(it) }

    override fun findAllMemberIdsByCohortIdAndAuthorityId(
        cohortId: CohortId,
        authorityId: AuthorityId,
    ): List<MemberId> {
        val cohortValue =
            dsl
                .select(COHORTS.VALUE)
                .from(COHORTS)
                .where(COHORTS.COHORT_ID.eq(cohortId.value))
                .fetchOne(COHORTS.VALUE)
                ?: return emptyList()
        val roleName = roleTypeFromLegacyAuthorityId(authorityId).code

        return dsl
            .selectDistinct(MEMBERS.MEMBER_ID)
            .from(MEMBERS)
            .join(MEMBER_COHORTS)
            .on(MEMBER_COHORTS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(MEMBER_ROLES)
            .on(MEMBER_ROLES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(ROLES)
            .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
            .where(MEMBER_COHORTS.COHORT_ID.eq(cohortId.value))
            .and(MEMBERS.DELETED_AT.isNull)
            .and(MEMBER_ROLES.DELETED_AT.isNull)
            .and(ROLES.NAME.eq(roleName))
            .and(org.jooq.impl.DSL.field(org.jooq.impl.DSL.name("member_roles", "cohort_id"), Long::class.java).eq(cohortId.value))
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
        val latestMemberCohorts = latestMemberCohorts()
        val latestMemberCohortIdField = latestMemberCohortsFieldId(latestMemberCohorts)
        val latestMemberCohortMemberIdField = latestMemberCohortsFieldMemberId(latestMemberCohorts)
        val latestMemberTeams = latestMemberTeams()
        val latestMemberTeamIdField = latestMemberTeamsFieldId(latestMemberTeams)
        val latestMemberTeamMemberIdField = latestMemberTeamsFieldMemberId(latestMemberTeams)
        val latestCohortValueField = COHORTS.VALUE.`as`("cohort_value")
        val latestCohortIdField = COHORTS.COHORT_ID.`as`("cohort_id")
        val latestTeamNumberField = TEAMS.NUMBER.`as`("team_number")

        val isAdminField =
            exists(
                selectOne()
                    .from(MEMBER_ROLES)
                    .join(ROLES)
                    .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
                    .where(MEMBER_ROLES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                    .and(MEMBER_ROLES.DELETED_AT.isNull)
                    .and(ROLES.NAME.eq(RoleType.Organizer.code))
                    .and(org.jooq.impl.DSL.field(org.jooq.impl.DSL.name("member_roles", "cohort_id"), Long::class.java).eq(latestCohortIdField)),
            ).`as`("is_admin")

        val statusPriority =
            `when`(MEMBERS.STATUS.eq("PENDING"), 0)
                .`when`(MEMBERS.STATUS.eq("ACTIVE"), 1)
                .`when`(MEMBERS.STATUS.eq("INACTIVE"), 2)
                .otherwise(3)

        val hasLatestCohortCondition =
            COHORTS.COHORT_ID.eq(latestCohortId)

        val filterCondition =
            when (latest) {
                true -> hasLatestCohortCondition
                false -> hasLatestCohortCondition.not()
                null -> noCondition()
            }

        return dsl
            .select(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                MEMBERS.STATUS,
                MEMBERS.PART,
                latestCohortValueField,
                latestCohortIdField,
                latestTeamNumberField,
                isAdminField,
            ).from(MEMBERS)
            .leftJoin(latestMemberCohorts)
            .on(latestMemberCohortMemberIdField.eq(MEMBERS.MEMBER_ID))
            .leftJoin(MEMBER_COHORTS)
            .on(MEMBER_COHORTS.MEMBER_COHORT_ID.eq(latestMemberCohortIdField))
            .leftJoin(COHORTS)
            .on(COHORTS.COHORT_ID.eq(MEMBER_COHORTS.COHORT_ID))
            .leftJoin(latestMemberTeams)
            .on(latestMemberTeamMemberIdField.eq(MEMBERS.MEMBER_ID))
            .leftJoin(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_TEAM_ID.eq(latestMemberTeamIdField))
            .leftJoin(TEAMS)
            .on(TEAMS.TEAM_ID.eq(MEMBER_TEAMS.TEAM_ID))
            .where(
                MEMBERS.DELETED_AT.isNull
                    .and(filterCondition),
            ).orderBy(
                // nullsLast() 가 별칭을 case 표현식 안에 넣어 렌더링하므로 여기서도 한정된 실제 컬럼을 쓴다.
                COHORTS.COHORT_ID.desc().nullsLast(),
                statusPriority.asc(),
                MEMBERS.NAME.asc(),
            ).fetch { record ->
                MemberOverviewQueryModel(
                    memberId = requireNotNull(record[MEMBERS.MEMBER_ID]),
                    cohortId = record[latestCohortIdField],
                    cohortValue = record[latestCohortValueField],
                    name = record[MEMBERS.NAME] ?: "",
                    teamNumber = TeamNumber(record[latestTeamNumberField] ?: 0),
                    isAdmin = record[isAdminField] ?: false,
                    status = record[MEMBERS.STATUS] ?: "",
                    part = record[MEMBERS.PART],
                )
            }
    }

    override fun findMemberTeamNumberByMemberIds(memberIds: List<MemberId>): Map<Long, Int> {
        if (memberIds.isEmpty()) return emptyMap()

        // 멤버별 가장 최신(max) member_team_id를 서브쿼리로 구함
        val maxMemberTeamId =
            DSL.select(
                MEMBER_TEAMS.MEMBER_ID,
                DSL.max(MEMBER_TEAMS.MEMBER_TEAM_ID).`as`("max_id"),
            )
                .from(MEMBER_TEAMS)
                .where(MEMBER_TEAMS.MEMBER_ID.`in`(memberIds.map { it.value }))
                .groupBy(MEMBER_TEAMS.MEMBER_ID)
                .asTable("latest_mt")

        return dsl
            .select(MEMBER_TEAMS.MEMBER_ID, TEAMS.NUMBER)
            .from(MEMBER_TEAMS)
            .join(TEAMS).on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .join(maxMemberTeamId)
            .on(
                MEMBER_TEAMS.MEMBER_TEAM_ID.eq(
                    maxMemberTeamId.field("max_id", Long::class.java),
                ),
            )
            .fetch()
            .mapNotNull { record ->
                val memberId = record[MEMBER_TEAMS.MEMBER_ID] ?: return@mapNotNull null
                val teamNumber = record[TEAMS.NUMBER] ?: return@mapNotNull null
                memberId to teamNumber
            }
            .toMap()
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

    override fun findMemberTeamIdByMemberIdAndCohortId(
        memberId: MemberId,
        cohortId: CohortId,
    ): Long? =
        dsl.select(TEAMS.TEAM_ID).from(MEMBER_TEAMS)
            .join(TEAMS).on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(MEMBER_TEAMS.MEMBER_ID.eq(memberId.value), TEAMS.COHORT_ID.eq(cohortId.value))
            .orderBy(MEMBER_TEAMS.MEMBER_TEAM_ID.desc())
            .limit(1)
            .fetchOne(TEAMS.TEAM_ID)

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
            .where(field(name("member_id"), Long::class.java).eq(value))
            .execute()
        dsl.deleteFrom(MEMBERS)
            .where(MEMBERS.MEMBER_ID.eq(value))
            .execute()
    }

    override fun findAll(): List<Member> = memberJpaRepository.findAllByDeletedAtIsNull().map { it.toDomain() }

    private fun roleTypeFromLegacyAuthorityId(authorityId: AuthorityId): RoleType =
        when (authorityId.value) {
            LEGACY_DEEPER_AUTHORITY_ID -> RoleType.Deeper
            LEGACY_ORGANIZER_AUTHORITY_ID -> RoleType.Organizer
            else -> RoleType.Guest
        }

    /**
     * 멤버별 대표 소속(회원 목록의 기수 표시 등)을 member_cohort_id(입력 순서)가 아니라 가장 높은 기수로 결정한다.
     * 과거 기수 행이 나중에 추가돼도(예: 기수 init API) 대표 기수가 바뀌지 않도록 하기 위함.
     * 같은 기수 행이 중복된 경우에만 member_cohort_id 최댓값을 사용한다.
     */
    private fun latestMemberCohorts(memberIds: Collection<Long>? = null): org.jooq.Table<*> {
        val memberIdCondition =
            if (memberIds.isNullOrEmpty()) {
                noCondition()
            } else {
                MEMBER_COHORTS.MEMBER_ID.`in`(memberIds)
            }
        val highestCohorts =
            DSL.select(
                MEMBER_COHORTS.MEMBER_ID,
                max(MEMBER_COHORTS.COHORT_ID).`as`("highest_cohort_id"),
            )
                .from(MEMBER_COHORTS)
                .where(memberIdCondition)
                .groupBy(MEMBER_COHORTS.MEMBER_ID)
                .asTable("highest_member_cohorts")
        val memberCohorts = MEMBER_COHORTS.`as`("mc")

        return DSL.select(
            memberCohorts.MEMBER_ID,
            max(memberCohorts.MEMBER_COHORT_ID).`as`("latest_member_cohort_id"),
        )
            .from(memberCohorts)
            .join(highestCohorts)
            .on(highestCohorts.field(MEMBER_COHORTS.MEMBER_ID)!!.eq(memberCohorts.MEMBER_ID))
            .and(highestCohorts.field(name("highest_cohort_id"), Long::class.java)!!.eq(memberCohorts.COHORT_ID))
            .groupBy(memberCohorts.MEMBER_ID)
            .asTable("latest_member_cohorts")
    }

    private fun latestMemberTeams(memberIds: Collection<Long>? = null) =
        DSL.select(
            MEMBER_TEAMS.MEMBER_ID,
            max(MEMBER_TEAMS.MEMBER_TEAM_ID).`as`("latest_member_team_id"),
        )
            .from(MEMBER_TEAMS)
            .where(
                if (memberIds.isNullOrEmpty()) {
                    noCondition()
                } else {
                    MEMBER_TEAMS.MEMBER_ID.`in`(memberIds)
                },
            )
            .groupBy(MEMBER_TEAMS.MEMBER_ID)
            .asTable("latest_member_teams")

    private fun latestMemberCohortsFieldId(table: org.jooq.Table<*> = latestMemberCohorts()) =
        table.field(name("latest_member_cohort_id"), Long::class.java)!!

    private fun latestMemberCohortsFieldMemberId(table: org.jooq.Table<*> = latestMemberCohorts()) =
        table.field(name(MEMBER_COHORTS.MEMBER_ID.name), Long::class.java)!!

    private fun latestMemberTeamsFieldId(table: org.jooq.Table<*> = latestMemberTeams()) =
        table.field(name("latest_member_team_id"), Long::class.java)!!

    private fun latestMemberTeamsFieldMemberId(table: org.jooq.Table<*> = latestMemberTeams()) =
        table.field(MEMBER_TEAMS.MEMBER_ID)!!


    companion object {
        private const val LEGACY_DEEPER_AUTHORITY_ID = 1L
        private const val LEGACY_ORGANIZER_AUTHORITY_ID = 2L
    }
}

package core.persistence.attendance.repository

import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import core.domain.attendance.port.outbound.query.MemberAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberSessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MyDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionRosterQueryModel
import core.domain.team.vo.TeamNumber
import core.entity.attendance.AttendanceEntity
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SelectJoinStep
import org.jooq.Table
import org.jooq.dsl.tables.references.ABSENCE_REASONS
import org.jooq.dsl.tables.references.ABSENCE_REASON_IMAGES
import org.jooq.dsl.tables.references.ATTENDANCES
import org.jooq.dsl.tables.references.COHORTS
import org.jooq.dsl.tables.references.IMAGES
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_COHORTS
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.MEMBER_TEAMS
import org.jooq.dsl.tables.references.ROLES
import org.jooq.dsl.tables.references.SESSIONS
import org.jooq.dsl.tables.references.TEAMS
import org.jooq.impl.DSL
import org.jooq.impl.DSL.exists
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.notExists
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.selectCount
import org.jooq.impl.DSL.selectOne
import org.jooq.impl.DSL.sum
import org.jooq.impl.DSL.`when`
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneId

@Repository
class AttendanceRepository(
    private val attendanceJpaRepository: AttendanceJpaRepository,
    private val dsl: DSLContext,
) : AttendancePersistencePort {
    override fun save(attendance: Attendance) {
        attendanceJpaRepository.save(AttendanceEntity.from(attendance))
    }

    override fun findAttendanceBy(
        sessionId: Long,
        memberId: Long,
    ): Attendance? =
        attendanceJpaRepository
            .findBySessionIdAndMemberIdAndDeletedAtIsNull(
                sessionId,
                memberId,
            )?.toDomain()

    override fun findAllBySessionId(sessionId: Long): List<Attendance> =
        attendanceJpaRepository.findAllBySessionIdAndDeletedAtIsNull(sessionId).map { it.toDomain() }

    /** 대상은 [sessionRosterConditions] 로 고르고 팀과 결석 사유서는 스칼라 서브쿼리로 붙여 행이 늘지 않는다. */
    override fun findSessionRoster(
        sessionId: Long,
        cohortId: Long,
    ): List<SessionRosterQueryModel> {
        val isAdminField = isAdminField()
        val teamNumber = teamNumberInCohort(MEMBERS.MEMBER_ID, cohortId)
        val teamNumberField = teamNumber.`as`(TEAM_NUMBER)
        val absenceReasonField =
            field(
                select(ABSENCE_REASONS.CONTENTS)
                    .from(ABSENCE_REASONS)
                    .where(
                        ABSENCE_REASONS.SESSION_ID.eq(ATTENDANCES.SESSION_ID),
                        ABSENCE_REASONS.MEMBER_ID.eq(ATTENDANCES.MEMBER_ID),
                    ).orderBy(ABSENCE_REASONS.ABSENCE_REASON_ID.desc())
                    .limit(1),
            ).`as`(ABSENCE_REASON)
        // updated_at 은 생성 타입이 LocalDateTime 이라 존을 가정하면 어긋난다. JPA 가 쓴 것과 같은 JDBC 경로(attended_at 과 같음)로 읽는다.
        val updatedAtField = ATTENDANCES.UPDATED_AT.coerce(SQLDataType.INSTANT)

        return dsl
            .select(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                teamNumberField,
                isAdminField,
                MEMBERS.PART,
                ATTENDANCES.STATUS,
                ATTENDANCES.ATTENDED_AT,
                updatedAtField,
                absenceReasonField,
            ).from(ATTENDANCES)
            .joinSessionAndMember()
            .join(COHORTS)
            .on(SESSIONS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .where(sessionRosterConditions(sessionId, cohortId))
            .orderBy(teamNumber.asc().nullsLast(), MEMBERS.NAME.asc(), MEMBERS.MEMBER_ID.asc())
            .fetch { record ->
                SessionRosterQueryModel(
                    memberId = record[MEMBERS.MEMBER_ID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = record[teamNumberField],
                    isAdmin = record[isAdminField] ?: false,
                    part = record[MEMBERS.PART],
                    attendanceStatus = record[ATTENDANCES.STATUS]!!,
                    attendedAt = record[ATTENDANCES.ATTENDED_AT],
                    updatedAt = record[updatedAtField],
                    absenceReason = record[absenceReasonField],
                )
            }
    }

    private fun <R : Record> SelectJoinStep<R>.joinSessionAndMember(): SelectJoinStep<R> =
        join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))

    /**
     * 운영진 세션 명단의 대상 조건. [joinSessionAndMember] 를 전제로 한다.
     * 현재 기수 소속(EXISTS 라 소속 중복으로 행이 늘지 않음)이고 삭제되지 않은 멤버의 살아 있는 출석 기록만 본다.
     * 멤버 삭제는 hard delete 라 MEMBERS 내부 조인으로 고아 기록도 빠진다.
     * 같은 (세션, 멤버)의 살아 있는 기록이 여러 개면 attendance_id 가 가장 큰 것만 본다.
     */
    private fun sessionRosterConditions(
        sessionId: Long,
        cohortId: Long,
    ): List<Condition> {
        val newer = ATTENDANCES.`as`(NEWER_ATTENDANCE)
        return listOf(
            ATTENDANCES.SESSION_ID.eq(sessionId),
            ATTENDANCES.DELETED_AT.isNull,
            notExists(
                selectOne()
                    .from(newer)
                    .where(
                        newer.SESSION_ID.eq(ATTENDANCES.SESSION_ID),
                        newer.MEMBER_ID.eq(ATTENDANCES.MEMBER_ID),
                        newer.DELETED_AT.isNull,
                        newer.ATTENDANCE_ID.gt(ATTENDANCES.ATTENDANCE_ID),
                    ),
            ),
            SESSIONS.COHORT_ID.eq(cohortId),
            SESSIONS.DELETED_AT.isNull,
            MEMBERS.DELETED_AT.isNull,
            exists(
                selectOne()
                    .from(MEMBER_COHORTS)
                    .where(MEMBER_COHORTS.MEMBER_ID.eq(MEMBERS.MEMBER_ID), MEMBER_COHORTS.COHORT_ID.eq(cohortId)),
            ),
        )
    }

    override fun findTeamNumberInCohort(
        memberId: Long,
        cohortId: Long,
    ): Int? =
        dsl
            .select(TEAMS.NUMBER)
            .from(MEMBER_TEAMS)
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(MEMBER_TEAMS.MEMBER_ID.eq(memberId), TEAMS.COHORT_ID.eq(cohortId))
            .orderBy(MEMBER_TEAMS.MEMBER_TEAM_ID.desc())
            .limit(1)
            .fetchOne(TEAMS.NUMBER)

    override fun findMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: TeamNumber,
    ): List<MemberAttendanceQueryModel> {
        val isAdminField = isAdminField()

        return dsl
            .select(memberSummaryFields(isAdminField))
            .from(attendanceSummary)
            .joinMemberCohort()
            .where(memberAttendanceConditions(query, myTeamNumber))
            .orderBy(summaryTeamNumber.asc(), MEMBERS.NAME.asc(), MEMBERS.MEMBER_ID.asc())
            .limit(query.size)
            .offset((query.page - 1) * query.size)
            .fetch { record ->
                MemberAttendanceQueryModel(
                    id = record[MEMBERS.MEMBER_ID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = TeamNumber(record[summaryTeamNumber]),
                    isAdmin = record[isAdminField] ?: false,
                    part = record[MEMBERS.PART],
                    summary = record.toAttendanceSummary(),
                )
            }
    }

    override fun findDetailAttendanceBySession(
        query: GetDetailAttendanceBySessionQuery,
    ): SessionDetailAttendanceQueryModel? {
        val isAdminField = isAdminField()

        return dsl
            .select(
                memberSummaryFields(isAdminField) +
                    listOf(
                        SESSIONS.SESSION_ID,
                        SESSIONS.WEEK,
                        SESSIONS.EVENT_NAME,
                        SESSIONS.DATE,
                        ATTENDANCES.STATUS,
                        ATTENDANCES.ATTENDED_AT,
                        ATTENDANCES.UPDATED_AT,
                    ),
            ).from(ATTENDANCES)
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(attendanceSummary)
            .on(summaryMemberId.eq(ATTENDANCES.MEMBER_ID), summaryCohortId.eq(SESSIONS.COHORT_ID))
            .joinMemberCohort()
            .where(detailAttendanceConditions(query))
            .fetchOne {
                SessionDetailAttendanceQueryModel(
                    memberId = it[MEMBERS.MEMBER_ID]!!,
                    memberName = it[MEMBERS.NAME]!!,
                    teamNumber = TeamNumber(it[summaryTeamNumber]),
                    isAdmin = it[isAdminField] ?: false,
                    part = it[MEMBERS.PART],
                    summary = it.toAttendanceSummary(),
                    sessionId = it[SESSIONS.SESSION_ID]!!,
                    sessionWeek = it[SESSIONS.WEEK]!!,
                    sessionEventName = it[SESSIONS.EVENT_NAME]!!,
                    sessionDate = it[SESSIONS.DATE]!!,
                    attendanceStatus = it[ATTENDANCES.STATUS]!!,
                    attendedAt =
                        it[ATTENDANCES.ATTENDED_AT]
                            ?.atZone(ZoneId.of("UTC"))
                            ?.toInstant(),
                    updatedAt =
                        it[ATTENDANCES.UPDATED_AT]
                            ?.atZone(ZoneId.of("UTC"))
                            ?.toInstant(),
                )
            }
    }

    override fun findDetailMemberAttendance(
        query: GetDetailMemberAttendancesQuery,
    ): List<MemberDetailAttendanceQueryModel> {
        val isAdminField = isAdminField()

        return dsl
            .select(memberSummaryFields(isAdminField))
            .from(attendanceSummary)
            .joinMemberCohort()
            .where(
                summaryMemberId.eq(query.memberId.value),
                summaryCohortId.eq(latestAttendedCohortId(query.memberId.value)),
            ).fetch {
                MemberDetailAttendanceQueryModel(
                    memberId = it[MEMBERS.MEMBER_ID]!!,
                    memberName = it[MEMBERS.NAME]!!,
                    teamNumber = TeamNumber(it[summaryTeamNumber]),
                    isAdmin = it[isAdminField] ?: false,
                    part = it[MEMBERS.PART],
                    summary = it.toAttendanceSummary(),
                )
            }
    }

    override fun findMemberSessionAttendances(
        query: GetDetailMemberAttendancesQuery,
    ): List<MemberSessionAttendanceQueryModel> {
        val records =
            dsl
                .select(
                    SESSIONS.SESSION_ID,
                    SESSIONS.WEEK,
                    SESSIONS.EVENT_NAME,
                    SESSIONS.DATE,
                    SESSIONS.IS_ONLINE,
                    SESSIONS.PLACE,
                    ATTENDANCES.STATUS,
                    ATTENDANCES.ATTENDED_AT,
                ).from(ATTENDANCES)
                .join(SESSIONS)
                .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
                .join(MEMBERS)
                .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                .where(detailMemberAttendanceConditions(query))
                .orderBy(SESSIONS.WEEK.asc(), SESSIONS.DATE.asc())
                .fetch()

        val absenceReasons =
            findAbsenceReasonsBySession(query.memberId.value, records.map { it[SESSIONS.SESSION_ID]!! })

        return records.map { record ->
            MemberSessionAttendanceQueryModel(
                sessionId = record[SESSIONS.SESSION_ID]!!,
                sessionWeek = record[SESSIONS.WEEK]!!,
                sessionEventName = record[SESSIONS.EVENT_NAME]!!,
                sessionDate = record[SESSIONS.DATE]!!,
                sessionIsOnline = record[SESSIONS.IS_ONLINE]!!,
                sessionPlace = record[SESSIONS.PLACE].orEmpty(),
                sessionAttendanceStatus = record[ATTENDANCES.STATUS]!!,
                attendedAt = record[ATTENDANCES.ATTENDED_AT],
                absenceReason = absenceReasons[record[SESSIONS.SESSION_ID]!!],
            )
        }
    }

    /** 결석 사유서를 한 번에 조회한다. 같은 세션에 여러 건이면 가장 최근 것을 쓴다. 첨부 이미지는 사유서 id 로 묶어 한 번 더 읽는다. */
    private fun findAbsenceReasonsBySession(
        memberId: Long,
        sessionIds: List<Long>,
    ): Map<Long, MemberSessionAttendanceQueryModel.AbsenceReason> {
        if (sessionIds.isEmpty()) return emptyMap()

        val reasons =
            dsl
                .select(
                    ABSENCE_REASONS.ABSENCE_REASON_ID,
                    ABSENCE_REASONS.SESSION_ID,
                    ABSENCE_REASONS.CONTENTS,
                    ABSENCE_REASONS.STATUS,
                ).from(ABSENCE_REASONS)
                .where(
                    ABSENCE_REASONS.MEMBER_ID.eq(memberId),
                    ABSENCE_REASONS.SESSION_ID.`in`(sessionIds),
                ).orderBy(ABSENCE_REASONS.ABSENCE_REASON_ID.asc())
                .fetch()
                .associate { record ->
                    record[ABSENCE_REASONS.SESSION_ID]!! to
                        MemberSessionAttendanceQueryModel.AbsenceReason(
                            id = record[ABSENCE_REASONS.ABSENCE_REASON_ID]!!,
                            contents = record[ABSENCE_REASONS.CONTENTS]!!,
                            status = record[ABSENCE_REASONS.STATUS]!!,
                        )
                }
        if (reasons.isEmpty()) return reasons

        val images = findImagesByAbsenceReason(reasons.values.map { it.id })
        return reasons.mapValues { (_, reason) -> reason.copy(images = images[reason.id].orEmpty()) }
    }

    private fun findImagesByAbsenceReason(
        absenceReasonIds: List<Long>,
    ): Map<Long, List<MemberSessionAttendanceQueryModel.Image>> =
        dsl
            .select(ABSENCE_REASON_IMAGES.ABSENCE_REASON_ID, ABSENCE_REASON_IMAGES.IMAGE_ID, IMAGES.ORIGINAL_FILE_NAME)
            .from(ABSENCE_REASON_IMAGES)
            .leftJoin(IMAGES)
            .on(ABSENCE_REASON_IMAGES.IMAGE_ID.eq(IMAGES.IMAGE_ID))
            .where(ABSENCE_REASON_IMAGES.ABSENCE_REASON_ID.`in`(absenceReasonIds))
            .orderBy(ABSENCE_REASON_IMAGES.ABSENCE_REASON_ID.asc(), ABSENCE_REASON_IMAGES.DISPLAY_ORDER.asc())
            .fetch()
            .groupBy({ it[ABSENCE_REASON_IMAGES.ABSENCE_REASON_ID]!! }) {
                MemberSessionAttendanceQueryModel.Image(
                    imageId = it[ABSENCE_REASON_IMAGES.IMAGE_ID]!!,
                    fileName = it[IMAGES.ORIGINAL_FILE_NAME],
                )
            }

    override fun findMyDetailAttendanceBySession(query: GetMyAttendanceBySessionQuery): MyDetailAttendanceQueryModel? =
        dsl
            .select(
                ATTENDANCES.STATUS,
                ATTENDANCES.ATTENDED_AT,
                SESSIONS.WEEK,
                SESSIONS.EVENT_NAME,
                SESSIONS.DATE,
                SESSIONS.PLACE,
            ).from(ATTENDANCES)
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .where(myAttendanceConditions(query))
            .fetchOne {
                MyDetailAttendanceQueryModel(
                    attendanceStatus = it[ATTENDANCES.STATUS]!!,
                    attendedAt =
                        it[ATTENDANCES.ATTENDED_AT]
                            ?.atZone(ZoneId.of("Asia/Seoul"))
                            ?.toInstant(),
                    sessionWeek = it[SESSIONS.WEEK]!!,
                    sessionEventName = it[SESSIONS.EVENT_NAME]!!,
                    sessionDate = it[SESSIONS.DATE]!!,
                    sessionPlace = it[SESSIONS.PLACE]!!,
                )
            }

    // jOOQ DSLContext 는 JPA 트랜잭션 밖 커넥션을 쓰므로, 호출 트랜잭션과 함께 커밋되도록 JPA 로 저장한다.
    override fun saveInBatch(attendances: List<Attendance>) {
        if (attendances.isEmpty()) return
        attendanceJpaRepository.saveAll(attendances.map { AttendanceEntity.from(it) })
    }

    override fun recordAttendanceIfAllowed(
        attendanceId: Long,
        status: AttendanceStatus,
        attendedAt: Instant,
    ): Boolean = attendanceJpaRepository.recordAttendanceIfAllowed(attendanceId, status.name, attendedAt) == 1

    override fun updateStatusByAdmin(
        sessionId: Long,
        memberIds: List<Long>,
        status: AttendanceStatus,
        updatedAt: Instant,
    ): Int {
        if (memberIds.isEmpty()) return 0
        // 단일 UPDATE 라 행 잠금은 (session_id, member_id) 인덱스 순서로 잡힌다. 자동 결석 UPDATE 와 같은 순서다.
        val sortedMemberIds = memberIds.distinct().sorted()
        return attendanceJpaRepository.updateStatusByAdmin(sessionId, sortedMemberIds, status.name, updatedAt)
    }

    override fun countActiveAttendances(
        sessionId: Long,
        memberIds: List<Long>,
    ): Int {
        if (memberIds.isEmpty()) return 0
        return attendanceJpaRepository.countActiveMembers(sessionId, memberIds.distinct()).toInt()
    }

    override fun updateStatusByPolicy(
        attendanceId: Long,
        expectedStatus: AttendanceStatus,
        newStatus: AttendanceStatus,
    ): Boolean = attendanceJpaRepository.updateStatusByPolicy(attendanceId, expectedStatus.name, newStatus.name) == 1

    override fun reopenAutoAbsence(attendanceId: Long): Boolean =
        attendanceJpaRepository.reopenAutoAbsence(attendanceId) == 1

    override fun markAutoAbsence(
        sessionId: Long,
        autoAbsentAt: Instant,
    ): Int = attendanceJpaRepository.markAutoAbsence(sessionId, autoAbsentAt)

    override fun softDeleteAllBySessionId(
        sessionId: Long,
        deletedAt: Instant,
    ): Int = attendanceJpaRepository.softDeleteAllBySessionId(sessionId, deletedAt)

    override fun countMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: TeamNumber,
    ): Int =
        dsl
            .selectCount()
            .from(attendanceSummary)
            .joinMemberCohort()
            .where(memberAttendanceConditions(query, myTeamNumber))
            .fetchOne(0, Int::class.java) ?: 0

    /**
     * 수료 판정 조회들이 공유하는 (멤버, 기수) 단위 출석 집계. 팀과 조인하지 않아 카운트가 곱해지지 않는다.
     * 삭제된 세션/기록은 빼고 분모로 쓸 기수 전체 세션 수(삭제 제외)를 함께 구한다.
     */
    private val attendanceSummary: Table<*> =
        run {
            val cohortSessions = SESSIONS.`as`("cohort_sessions")
            select(
                ATTENDANCES.MEMBER_ID.`as`(SUMMARY_MEMBER_ID),
                SESSIONS.COHORT_ID.`as`(SUMMARY_COHORT_ID),
                field(
                    selectCount()
                        .from(cohortSessions)
                        .where(cohortSessions.COHORT_ID.eq(SESSIONS.COHORT_ID), cohortSessions.DELETED_AT.isNull),
                ).`as`(TOTAL_SESSION_COUNT),
                countWhen(ATTENDANCES.STATUS.eq(AttendanceStatus.PRESENT.name)).`as`(PRESENT_COUNT),
                countWhen(ATTENDANCES.STATUS.eq(AttendanceStatus.LATE.name)).`as`(LATE_COUNT),
                countWhen(ATTENDANCES.STATUS.eq(AttendanceStatus.EXCUSED_ABSENT.name)).`as`(EXCUSED_ABSENT_COUNT),
                countWhen(
                    ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.IS_ONLINE.eq(true)),
                ).`as`(ONLINE_ABSENT_COUNT),
                countWhen(
                    ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.IS_ONLINE.eq(false)),
                ).`as`(OFFLINE_ABSENT_COUNT),
            ).from(ATTENDANCES)
                .join(SESSIONS)
                .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
                .where(ATTENDANCES.DELETED_AT.isNull, SESSIONS.DELETED_AT.isNull)
                .groupBy(ATTENDANCES.MEMBER_ID, SESSIONS.COHORT_ID)
                .asTable(SUMMARY)
        }

    private val summaryMemberId = attendanceSummary.field(SUMMARY_MEMBER_ID, Long::class.javaObjectType)!!
    private val summaryCohortId = attendanceSummary.field(SUMMARY_COHORT_ID, Long::class.javaObjectType)!!
    private val summaryTotalSessionCount = attendanceSummary.field(TOTAL_SESSION_COUNT, Int::class.javaObjectType)!!
    private val summaryPresentCount = attendanceSummary.field(PRESENT_COUNT, Int::class.javaObjectType)!!
    private val summaryLateCount = attendanceSummary.field(LATE_COUNT, Int::class.javaObjectType)!!
    private val summaryExcusedAbsentCount = attendanceSummary.field(EXCUSED_ABSENT_COUNT, Int::class.javaObjectType)!!
    private val summaryOnlineAbsentCount = attendanceSummary.field(ONLINE_ABSENT_COUNT, Int::class.javaObjectType)!!
    private val summaryOfflineAbsentCount = attendanceSummary.field(OFFLINE_ABSENT_COUNT, Int::class.javaObjectType)!!
    private val summaryFields: List<Field<*>> =
        listOf(
            summaryTotalSessionCount,
            summaryPresentCount,
            summaryLateCount,
            summaryExcusedAbsentCount,
            summaryOnlineAbsentCount,
            summaryOfflineAbsentCount,
        )

    private fun memberSummaryFields(isAdminField: Field<Boolean>): List<Field<*>> =
        listOf(MEMBERS.MEMBER_ID, MEMBERS.NAME, summaryTeamNumber, isAdminField, MEMBERS.PART) + summaryFields

    private fun countWhen(condition: Condition) = sum(`when`(condition, inline(1)).otherwise(inline(0)))

    private fun Record.toAttendanceSummary() =
        AttendanceSummaryQueryModel(
            totalSessionCount = this[summaryTotalSessionCount] ?: 0,
            presentCount = this[summaryPresentCount] ?: 0,
            lateCount = this[summaryLateCount] ?: 0,
            excusedAbsentCount = this[summaryExcusedAbsentCount] ?: 0,
            onlineAbsentCount = this[summaryOnlineAbsentCount] ?: 0,
            offlineAbsentCount = this[summaryOfflineAbsentCount] ?: 0,
        )

    /** 팀은 조인하지 않아 팀 매핑 수와 무관하게 (멤버, 기수)당 한 행이다. */
    private fun <R : Record> SelectJoinStep<R>.joinMemberCohort(): SelectJoinStep<R> =
        join(MEMBERS)
            .on(MEMBERS.MEMBER_ID.eq(summaryMemberId))
            .join(COHORTS)
            .on(COHORTS.COHORT_ID.eq(summaryCohortId))

    /** 표시용 팀 번호: 그 기수의 가장 최근 배정(member_team_id 최대). 없으면 null(팀 0). */
    private val latestTeamNumber =
        field(
            select(TEAMS.NUMBER)
                .from(MEMBER_TEAMS)
                .join(TEAMS)
                .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
                .where(MEMBER_TEAMS.MEMBER_ID.eq(summaryMemberId), TEAMS.COHORT_ID.eq(summaryCohortId))
                .orderBy(MEMBER_TEAMS.MEMBER_TEAM_ID.desc())
                .limit(1),
        )

    private val summaryTeamNumber = latestTeamNumber.`as`(TEAM_NUMBER)

    /** 그 기수에서 멤버의 가장 최근 배정(member_team_id 최대) 팀 번호. 없으면 null */
    private fun teamNumberInCohort(
        memberId: Field<Long?>,
        cohortId: Long,
    ): Field<Int?> =
        field(
            select(TEAMS.NUMBER)
                .from(MEMBER_TEAMS)
                .join(TEAMS)
                .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
                .where(MEMBER_TEAMS.MEMBER_ID.eq(memberId), TEAMS.COHORT_ID.eq(cohortId))
                .orderBy(MEMBER_TEAMS.MEMBER_TEAM_ID.desc())
                .limit(1),
        )

    /** 표시하는 팀(그 기수의 최신 배정) 기준으로 멤버만 고른다. 같은 기수의 이전 팀 배정으로는 걸리지 않는다. */
    private fun belongsToTeamIn(teamNumbers: Collection<Int>): Condition = latestTeamNumber.`in`(teamNumbers)

    private fun isAdminField() =
        exists(
            selectOne()
                .from(MEMBER_ROLES)
                .join(ROLES)
                .on(MEMBER_ROLES.ROLE_ID.eq(ROLES.ROLE_ID))
                .where(MEMBER_ROLES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                .and(MEMBER_ROLES.DELETED_AT.isNull)
                .and(ROLES.NAME.eq(COHORTS.VALUE.concat(inline("기 운영진")))),
        ).`as`("is_admin")

    companion object {
        private const val LATE_COUNT = "late_count"
        private const val ONLINE_ABSENT_COUNT = "online_absent_count"
        private const val OFFLINE_ABSENT_COUNT = "offline_absent_count"
        private const val PRESENT_COUNT = "present_count"
        private const val EXCUSED_ABSENT_COUNT = "excused_absent_count"
        private const val TOTAL_SESSION_COUNT = "total_session_count"
        private const val TEAM_NUMBER = "team_number"
        private const val ABSENCE_REASON = "absence_reason"
        private const val NEWER_ATTENDANCE = "newer_attendance"
        private const val SUMMARY = "attendance_summary"
        private const val SUMMARY_MEMBER_ID = "member_id"
        private const val SUMMARY_COHORT_ID = "cohort_id"
    }

    private fun memberAttendanceConditions(
        query: GetMemberAttendancesQuery,
        myTeamNumber: TeamNumber,
    ): List<Condition> {
        val conditions = mutableListOf<Condition>()

        // 상태 필터는 대상 멤버만 고른다. 수료 판정용 집계는 항상 그 기수의 전체 출석 기록으로 한다.
        query.statuses?.takeIf { it.isNotEmpty() }?.let { statuses ->
            conditions +=
                exists(
                    selectOne()
                        .from(ATTENDANCES)
                        .join(SESSIONS)
                        .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
                        .where(
                            ATTENDANCES.MEMBER_ID.eq(summaryMemberId),
                            SESSIONS.COHORT_ID.eq(summaryCohortId),
                            ATTENDANCES.STATUS.`in`(statuses.map { it.name }),
                            ATTENDANCES.DELETED_AT.isNull,
                            SESSIONS.DELETED_AT.isNull,
                        ),
                )
        }

        val teams = query.teams.orEmpty()
        when {
            query.onlyMyTeam == true -> conditions += belongsToTeamIn(listOf(myTeamNumber.value))
            teams.isNotEmpty() -> conditions += belongsToTeamIn(teams)
        }

        query.name?.takeIf { it.isNotBlank() }?.let { name ->
            conditions += MEMBERS.NAME.containsIgnoreCase(name)
        }

        return conditions
    }

    private fun detailAttendanceConditions(query: GetDetailAttendanceBySessionQuery): List<Condition> =
        listOf(
            ATTENDANCES.SESSION_ID.eq(query.sessionId.value),
            ATTENDANCES.MEMBER_ID.eq(query.memberId.value),
            ATTENDANCES.DELETED_AT.isNull,
            SESSIONS.DELETED_AT.isNull,
        )

    private fun detailMemberAttendanceConditions(query: GetDetailMemberAttendancesQuery): List<Condition> =
        listOf(
            ATTENDANCES.MEMBER_ID.eq(query.memberId.value),
            ATTENDANCES.DELETED_AT.isNull,
            SESSIONS.DELETED_AT.isNull,
            SESSIONS.COHORT_ID.eq(latestAttendedCohortId(query.memberId.value)),
        )

    /**
     * 해당 멤버가 출석 기록을 가진 기수 중 가장 최신(기수 값이 가장 큰) 기수의 cohortId.
     * 멤버가 여러 기수에 걸쳐 출석한 경우 기수별로 행이 분리되어 통계가 다건이 되는 것을 막기 위해
     * 최신 기수 하나로 한정한다.
     */
    private fun latestAttendedCohortId(memberId: Long) =
        select(SESSIONS.COHORT_ID)
            .from(ATTENDANCES)
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(COHORTS)
            .on(SESSIONS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .where(
                ATTENDANCES.MEMBER_ID.eq(memberId),
                ATTENDANCES.DELETED_AT.isNull,
                SESSIONS.DELETED_AT.isNull,
            ).orderBy(
                DSL.cast(COHORTS.VALUE, SQLDataType.INTEGER).desc(),
                SESSIONS.COHORT_ID.desc(),
            ).limit(1)

    private fun myAttendanceConditions(query: GetMyAttendanceBySessionQuery): List<Condition> =
        listOf(
            ATTENDANCES.SESSION_ID.eq(query.sessionId.value),
            ATTENDANCES.MEMBER_ID.eq(query.memberId.value),
            ATTENDANCES.DELETED_AT.isNull,
        )
}

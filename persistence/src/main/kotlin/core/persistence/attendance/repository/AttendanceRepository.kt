package core.persistence.attendance.repository

import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.query.GetAttendancesBySessionWeekQuery
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.attendance.port.outbound.query.MemberAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberSessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MyDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionDetailAttendanceQueryModel
import core.domain.team.vo.TeamNumber
import core.entity.attendance.AttendanceEntity
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.ATTENDANCES
import org.jooq.dsl.tables.references.COHORTS
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.MEMBER_ROLES
import org.jooq.dsl.tables.references.MEMBER_TEAMS
import org.jooq.dsl.tables.references.ROLES
import org.jooq.dsl.tables.references.SESSIONS
import org.jooq.dsl.tables.references.TEAMS
import org.jooq.impl.DSL
import org.jooq.impl.DSL.exists
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.selectOne
import org.jooq.impl.DSL.sum
import org.jooq.impl.DSL.`when`
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
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

    override fun findSessionAttendancesByQuery(
        query: GetAttendancesBySessionWeekQuery,
        myTeamNumber: TeamNumber,
    ): List<SessionAttendanceQueryModel> {
        val isAdminField = isAdminField()

        return dsl
            .select(
                ATTENDANCES.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                isAdminField,
                MEMBERS.PART,
                ATTENDANCES.STATUS,
            ).from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(COHORTS)
            .on(SESSIONS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(
                sessionAttendanceConditions(query, myTeamNumber),
            ).orderBy(TEAMS.NUMBER.asc(), MEMBERS.NAME.asc(), ATTENDANCES.MEMBER_ID.asc())
            .limit(query.size)
            .offset((query.page - 1) * query.size)
            .fetch { record ->
                SessionAttendanceQueryModel(
                    id = record[ATTENDANCES.MEMBER_ID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = TeamNumber(record[TEAMS.NUMBER]!!),
                    isAdmin = record[isAdminField] ?: false,
                    part = record[MEMBERS.PART],
                    attendanceStatus = record[ATTENDANCES.STATUS]!!,
                )
            }
    }

    override fun findMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: TeamNumber,
    ): List<MemberAttendanceQueryModel> {
        val isAdminField = isAdminField()

        return dsl
            .select(
                ATTENDANCES.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                isAdminField,
                MEMBERS.PART,
                sum(
                    `when`(ATTENDANCES.STATUS.eq(AttendanceStatus.LATE.name), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`(LATE_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.IS_ONLINE.eq(true)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(ONLINE_ABSENT_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.IS_ONLINE.eq(false)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(OFFLINE_ABSENT_COUNT),
            ).from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(COHORTS)
            .on(SESSIONS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(
                memberAttendanceConditions(query, myTeamNumber),
            ).groupBy(
                ATTENDANCES.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
                COHORTS.VALUE,
            ).orderBy(TEAMS.NUMBER.asc(), MEMBERS.NAME.asc(), ATTENDANCES.MEMBER_ID.asc())
            .limit(query.size)
            .offset((query.page - 1) * query.size)
            .fetch { record ->
                MemberAttendanceQueryModel(
                    id = record[ATTENDANCES.MEMBER_ID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = TeamNumber(record[TEAMS.NUMBER]!!),
                    isAdmin = record[isAdminField] ?: false,
                    part = record[MEMBERS.PART],
                    lateCount = record.get(LATE_COUNT, Int::class.java) ?: 0,
                    onlineAbsentCount = record.get(ONLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                    offlineAbsentCount = record.get(OFFLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                )
            }
    }

    override fun findDetailAttendanceBySession(
        query: GetDetailAttendanceBySessionQuery,
    ): SessionDetailAttendanceQueryModel? {
        val isAdminField = isAdminField()

        return dsl
            .select(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                isAdminField,
                MEMBERS.PART,
                sum(
                    `when`(ATTENDANCES.STATUS.eq(AttendanceStatus.LATE.name), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`(LATE_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.IS_ONLINE.eq(true)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(ONLINE_ABSENT_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.IS_ONLINE.eq(false)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(OFFLINE_ABSENT_COUNT),
                SESSIONS.SESSION_ID,
                SESSIONS.WEEK,
                SESSIONS.EVENT_NAME,
                SESSIONS.DATE,
                ATTENDANCES.STATUS,
                ATTENDANCES.ATTENDED_AT,
                ATTENDANCES.UPDATED_AT,
            ).from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(COHORTS)
            .on(SESSIONS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(detailAttendanceConditions(query))
            .groupBy(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
                SESSIONS.SESSION_ID,
                SESSIONS.WEEK,
                SESSIONS.EVENT_NAME,
                SESSIONS.DATE,
                COHORTS.VALUE,
                ATTENDANCES.STATUS,
                ATTENDANCES.ATTENDED_AT,
                ATTENDANCES.UPDATED_AT,
            ).fetchOne {
                SessionDetailAttendanceQueryModel(
                    memberId = it[MEMBERS.MEMBER_ID]!!,
                    memberName = it[MEMBERS.NAME]!!,
                    teamNumber = TeamNumber(it[TEAMS.NUMBER]!!),
                    isAdmin = it[isAdminField] ?: false,
                    part = it[MEMBERS.PART],
                    lateCount = it.get(LATE_COUNT, Int::class.java) ?: 0,
                    onlineAbsentCount = it.get(ONLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                    offlineAbsentCount = it.get(OFFLINE_ABSENT_COUNT, Int::class.java) ?: 0,
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
            .select(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                isAdminField,
                MEMBERS.PART,
                sum(
                    `when`(ATTENDANCES.STATUS.eq(AttendanceStatus.PRESENT.name), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`(PRESENT_COUNT),
                sum(
                    `when`(ATTENDANCES.STATUS.eq(AttendanceStatus.LATE.name), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`(LATE_COUNT),
                sum(
                    `when`(ATTENDANCES.STATUS.eq(AttendanceStatus.EXCUSED_ABSENT.name), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`(EXCUSED_ABSENT_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.IS_ONLINE.eq(true)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(ONLINE_ABSENT_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.IS_ONLINE.eq(false)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(OFFLINE_ABSENT_COUNT),
                sum(
                    `when`(ATTENDANCES.STATUS.eq(AttendanceStatus.EARLY_LEAVE.name), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`(EARLY_LEAVE_COUNT),
            ).from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(COHORTS)
            .on(SESSIONS.COHORT_ID.eq(COHORTS.COHORT_ID))
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(detailMemberAttendanceConditions(query))
            .groupBy(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
                COHORTS.VALUE,
            ).fetch {
                MemberDetailAttendanceQueryModel(
                    memberId = it[MEMBERS.MEMBER_ID]!!,
                    memberName = it[MEMBERS.NAME]!!,
                    teamNumber = TeamNumber(it[TEAMS.NUMBER]!!),
                    isAdmin = it[isAdminField] ?: false,
                    part = it[MEMBERS.PART],
                    presentCount = it.get(PRESENT_COUNT, Int::class.java) ?: 0,
                    lateCount = it.get(LATE_COUNT, Int::class.java) ?: 0,
                    excusedAbsentCount = it.get(EXCUSED_ABSENT_COUNT, Int::class.java) ?: 0,
                    onlineAbsentCount = it.get(ONLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                    offlineAbsentCount = it.get(OFFLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                    earlyLeaveCount = it.get(EARLY_LEAVE_COUNT, Int::class.java) ?: 0,
                )
            }
    }

    override fun findMemberSessionAttendances(
        query: GetDetailMemberAttendancesQuery,
    ): List<MemberSessionAttendanceQueryModel> =
        dsl
            .select(
                SESSIONS.SESSION_ID,
                SESSIONS.WEEK,
                SESSIONS.EVENT_NAME,
                SESSIONS.DATE,
                ATTENDANCES.STATUS,
            ).from(ATTENDANCES)
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .where(detailMemberAttendanceConditions(query))
            .orderBy(SESSIONS.WEEK.asc(), SESSIONS.DATE.asc())
            .fetch { record ->
                MemberSessionAttendanceQueryModel(
                    sessionId = record[SESSIONS.SESSION_ID]!!,
                    sessionWeek = record[SESSIONS.WEEK]!!,
                    sessionEventName = record[SESSIONS.EVENT_NAME]!!,
                    sessionDate = record[SESSIONS.DATE]!!,
                    sessionAttendanceStatus = record[ATTENDANCES.STATUS]!!,
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

    override fun saveInBatch(attendances: List<Attendance>) {
        val records =
            attendances.map { attendance ->
                dsl.newRecord(ATTENDANCES).apply {
                    memberId = attendance.memberId.value
                    sessionId = attendance.sessionId.value
                    status = attendance.status.name
                    attendedAt = attendance.attendedAt
                }
            }

        dsl.batchInsert(records).execute()
    }

    override fun updateInBatch(attendances: List<Attendance>) {
        val records =
            attendances.map { attendance ->
                dsl.newRecord(ATTENDANCES).apply {
                    attendanceId = attendance.id?.value // ← PK 세팅
                    memberId = attendance.memberId.value
                    sessionId = attendance.sessionId.value
                    status = attendance.status.name
                    attendedAt = attendance.attendedAt
                    updatedAt = attendance.updatedAt?.atZone(ZoneId.of("UTC"))?.toLocalDateTime()
                    deletedAt = attendance.deletedAt?.atZone(ZoneId.of("UTC"))?.toLocalDateTime()
                }
            }

        dsl.batchUpdate(records).execute()
    }

    override fun countSessionAttendancesByQuery(
        query: GetAttendancesBySessionWeekQuery,
        myTeamNumber: TeamNumber,
    ): Int =
        dsl
            .selectCount()
            .from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(sessionAttendanceConditions(query, myTeamNumber))
            .fetchOne(0, Int::class.java) ?: 0

    override fun countMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: TeamNumber,
    ): Int =
        dsl
            .selectCount()
            .from(
                select(
                    ATTENDANCES.MEMBER_ID,
                    MEMBERS.NAME,
                    TEAMS.NUMBER,
                    MEMBERS.PART,
                ).from(ATTENDANCES)
                    .join(MEMBERS)
                    .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                    .join(SESSIONS)
                    .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
                    .join(MEMBER_TEAMS)
                    .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
                    .join(TEAMS)
                    .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
                    .where(memberAttendanceConditions(query, myTeamNumber))
                    .groupBy(
                        ATTENDANCES.MEMBER_ID,
                        MEMBERS.NAME,
                        TEAMS.NUMBER,
                        MEMBERS.PART,
                    ),
            ).fetchOne(0, Int::class.java) ?: 0

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
        private const val EARLY_LEAVE_COUNT = "early_leave_count"
    }

    private fun sessionAttendanceConditions(
        query: GetAttendancesBySessionWeekQuery,
        myTeamNumber: TeamNumber,
    ): List<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions += SESSIONS.SESSION_ID.eq(query.sessionId.value)
        conditions += ATTENDANCES.DELETED_AT.isNull

        query.statuses?.takeIf { it.isNotEmpty() }?.let { statuses ->
            conditions += ATTENDANCES.STATUS.`in`(statuses)
        }

        when {
            query.onlyMyTeam == true -> conditions += TEAMS.NUMBER.eq(myTeamNumber.value)
            query.teams?.isNotEmpty() == true -> conditions += TEAMS.NUMBER.`in`(query.teams)
        }

        query.name?.takeIf { it.isNotBlank() }?.let { name ->
            conditions += MEMBERS.NAME.containsIgnoreCase(name)
        }

        return conditions
    }

    private fun memberAttendanceConditions(
        query: GetMemberAttendancesQuery,
        myTeamNumber: TeamNumber,
    ): List<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions += ATTENDANCES.DELETED_AT.isNull

        query.statuses?.takeIf { it.isNotEmpty() }?.let { statuses ->
            conditions += ATTENDANCES.STATUS.`in`(statuses)
        }

        when {
            query.onlyMyTeam == true -> conditions += TEAMS.NUMBER.eq(myTeamNumber.value)
            query.teams?.isNotEmpty() == true -> conditions += TEAMS.NUMBER.`in`(query.teams)
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
        )

    private fun detailMemberAttendanceConditions(query: GetDetailMemberAttendancesQuery): List<Condition> =
        listOf(
            ATTENDANCES.MEMBER_ID.eq(query.memberId.value),
            ATTENDANCES.DELETED_AT.isNull,
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

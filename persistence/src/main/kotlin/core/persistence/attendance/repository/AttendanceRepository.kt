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
import core.entity.attendance.AttendanceEntity
import core.persistence.attendance.extension.toCondition
import jooq.dsl.tables.references.ATTENDANCES
import jooq.dsl.tables.references.MEMBERS
import jooq.dsl.tables.references.MEMBER_TEAMS
import jooq.dsl.tables.references.SESSIONS
import jooq.dsl.tables.references.TEAMS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.sum
import org.jooq.impl.DSL.`when`
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId

private const val PAGE_SIZE = 20

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
    ): Attendance? = attendanceJpaRepository.findBySessionIdAndMemberId(sessionId, memberId)?.toDomain()

    override fun findSessionAttendancesByQuery(
        query: GetAttendancesBySessionWeekQuery,
        myTeamNumber: Int?,
    ): List<SessionAttendanceQueryModel> =
        dsl
            .select(
                ATTENDANCES.MEMBERID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
                ATTENDANCES.STATUS,
            ).from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBERID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.MEMBERID.eq(SESSIONS.SESSION_ID))
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(
                query.toCondition(myTeamNumber),
            ).orderBy(TEAMS.NUMBER.asc(), MEMBERS.NAME.asc(), ATTENDANCES.MEMBERID.asc())
            .limit(PAGE_SIZE + 1)
            .fetch { record ->
                SessionAttendanceQueryModel(
                    id = record[ATTENDANCES.MEMBERID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = record[TEAMS.NUMBER]!!,
                    part = record[MEMBERS.PART]!!,
                    attendanceStatus = record[ATTENDANCES.STATUS]!!,
                )
            }

    override fun findMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: Int?,
    ): List<MemberAttendanceQueryModel> =
        dsl
            .select(
                ATTENDANCES.MEMBERID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
                sum(
                    `when`(ATTENDANCES.STATUS.eq(AttendanceStatus.LATE.name), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`(LATE_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.ISONLINE.eq(true)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(ONLINE_ABSENT_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.ISONLINE.eq(false)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(OFFLINE_ABSENT_COUNT),
            ).from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBERID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.SESSIONID.eq(SESSIONS.SESSION_ID))
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(
                query.toCondition(myTeamNumber),
            ).groupBy(
                ATTENDANCES.MEMBERID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
            ).orderBy(TEAMS.NUMBER.asc(), MEMBERS.NAME.asc(), ATTENDANCES.MEMBERID.asc())
            .limit(PAGE_SIZE + 1)
            .fetch { record ->
                MemberAttendanceQueryModel(
                    id = record[ATTENDANCES.MEMBERID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = record[TEAMS.NUMBER]!!,
                    part = record[MEMBERS.PART]!!,
                    lateCount = record.get(LATE_COUNT, Int::class.java) ?: 0,
                    onlineAbsentCount = record.get(ONLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                    offlineAbsentCount = record.get(OFFLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                )
            }

    override fun findDetailAttendanceBySession(
        query: GetDetailAttendanceBySessionQuery,
    ): SessionDetailAttendanceQueryModel? =
        dsl
            .select(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
                sum(
                    `when`(ATTENDANCES.STATUS.eq(AttendanceStatus.LATE.name), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`(LATE_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.ISONLINE.eq(true)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(ONLINE_ABSENT_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.ISONLINE.eq(false)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(OFFLINE_ABSENT_COUNT),
                SESSIONS.SESSION_ID,
                SESSIONS.WEEK,
                SESSIONS.EVENTNAME,
                SESSIONS.DATE,
                ATTENDANCES.STATUS,
                ATTENDANCES.ATTENDEDAT,
            ).from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBERID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.SESSIONID.eq(SESSIONS.SESSION_ID))
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(query.toCondition())
            .groupBy(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
                SESSIONS.SESSION_ID,
                SESSIONS.WEEK,
                SESSIONS.EVENTNAME,
                SESSIONS.DATE,
                ATTENDANCES.STATUS,
                ATTENDANCES.ATTENDEDAT,
            ).fetchOne {
                SessionDetailAttendanceQueryModel(
                    memberId = it[MEMBERS.MEMBER_ID]!!,
                    memberName = it[MEMBERS.NAME]!!,
                    teamNumber = it[TEAMS.NUMBER]!!,
                    part = it[MEMBERS.PART]!!,
                    lateCount = it.get(LATE_COUNT, Int::class.java) ?: 0,
                    onlineAbsentCount = it.get(ONLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                    offlineAbsentCount = it.get(OFFLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                    sessionId = it[SESSIONS.SESSION_ID]!!,
                    sessionWeek = it[SESSIONS.WEEK]!!,
                    sessionEventName = it[SESSIONS.EVENTNAME]!!,
                    sessionDate = it[SESSIONS.DATE]!!,
                    attendanceStatus = it[ATTENDANCES.STATUS]!!,
                    attendedAt = it[ATTENDANCES.ATTENDEDAT]
                        ?.atZone(ZoneId.of("Asia/Seoul"))
                        ?.toInstant(),
                )
            }

    override fun findDetailMemberAttendance(
        query: GetDetailMemberAttendancesQuery,
    ): MemberDetailAttendanceQueryModel? =
        dsl
            .select(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
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
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.ISONLINE.eq(true)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(ONLINE_ABSENT_COUNT),
                sum(
                    `when`(
                        ATTENDANCES.STATUS.eq(AttendanceStatus.ABSENT.name).and(SESSIONS.ISONLINE.eq(false)),
                        DSL.inline(1),
                    ).otherwise(DSL.inline(0)),
                ).`as`(OFFLINE_ABSENT_COUNT),
                sum(
                    `when`(ATTENDANCES.STATUS.eq(AttendanceStatus.EARLY_LEAVE.name), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`(EARLY_LEAVE_COUNT),
            ).from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBERID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.SESSIONID.eq(SESSIONS.SESSION_ID))
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(query.toCondition())
            .groupBy(
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
            ).fetchOne {
                MemberDetailAttendanceQueryModel(
                    memberId = it[MEMBERS.MEMBER_ID]!!,
                    memberName = it[MEMBERS.NAME]!!,
                    teamNumber = it[TEAMS.NUMBER]!!,
                    part = it[MEMBERS.PART]!!,
                    presentCount = it.get(PRESENT_COUNT, Int::class.java) ?: 0,
                    lateCount = it.get(LATE_COUNT, Int::class.java) ?: 0,
                    excusedAbsentCount = it.get(EXCUSED_ABSENT_COUNT, Int::class.java) ?: 0,
                    onlineAbsentCount = it.get(ONLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                    offlineAbsentCount = it.get(OFFLINE_ABSENT_COUNT, Int::class.java) ?: 0,
                    earlyLeaveCount = it.get(EARLY_LEAVE_COUNT, Int::class.java) ?: 0,
                )
            }

    override fun findMemberSessionAttendances(
        query: GetDetailMemberAttendancesQuery,
    ): List<MemberSessionAttendanceQueryModel> =
        dsl
            .select(
                SESSIONS.SESSION_ID,
                SESSIONS.WEEK,
                SESSIONS.EVENTNAME,
                SESSIONS.DATE,
                ATTENDANCES.STATUS,
            ).from(ATTENDANCES)
            .join(SESSIONS)
            .on(ATTENDANCES.SESSIONID.eq(SESSIONS.SESSION_ID))
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBERID.eq(MEMBERS.MEMBER_ID))
            .where(query.toCondition())
            .orderBy(SESSIONS.WEEK.asc(), SESSIONS.DATE.asc())
            .fetch { record ->
                MemberSessionAttendanceQueryModel(
                    sessionId = record[SESSIONS.SESSION_ID]!!,
                    sessionWeek = record[SESSIONS.WEEK]!!,
                    sessionEventName = record[SESSIONS.EVENTNAME]!!,
                    sessionDate = record[SESSIONS.DATE]!!,
                    sessionAttendanceStatus = record[ATTENDANCES.STATUS]!!,
                )
            }

    override fun findMyDetailAttendanceBySession(query: GetMyAttendanceBySessionQuery): MyDetailAttendanceQueryModel? =
        dsl
            .select(
                ATTENDANCES.STATUS,
                ATTENDANCES.ATTENDEDAT,
                SESSIONS.WEEK,
                SESSIONS.EVENTNAME,
                SESSIONS.DATE,
                SESSIONS.PLACE,
            ).from(ATTENDANCES)
            .join(SESSIONS)
            .on(ATTENDANCES.SESSIONID.eq(SESSIONS.SESSION_ID))
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBERID.eq(MEMBERS.MEMBER_ID))
            .where(query.toCondition())
            .fetchOne {
                MyDetailAttendanceQueryModel(
                    attendanceStatus = it[ATTENDANCES.STATUS]!!,
                    attendedAt = it[ATTENDANCES.ATTENDEDAT]
                        ?.atZone(ZoneId.of("Asia/Seoul"))
                        ?.toInstant(),
                    sessionWeek = it[SESSIONS.WEEK]!!,
                    sessionEventName = it[SESSIONS.EVENTNAME]!!,
                    sessionDate = it[SESSIONS.DATE]!!,
                    sessionPlace = it[SESSIONS.PLACE]!!,
                )
            }

    override fun saveInBatch(attendances: List<Attendance>) {
        val records =
            attendances.map { attendance ->
                dsl.newRecord(ATTENDANCES).apply {
                    memberid = attendance.memberId.value
                    sessionid = attendance.sessionId.value
                    status = attendance.status.name
                    attendedat = LocalDateTime.ofInstant(
                        attendance.attendedAt,
                        ZoneId.of(TIME_ZONE),
                    )
                }
            }

        dsl.batchInsert(records).execute()
    }

    companion object {
        private const val LATE_COUNT = "late_count"
        private const val ONLINE_ABSENT_COUNT = "online_absent_count"
        private const val OFFLINE_ABSENT_COUNT = "offline_absent_count"
        private const val PRESENT_COUNT = "present_count"
        private const val EXCUSED_ABSENT_COUNT = "excused_absent_count"
        private const val EARLY_LEAVE_COUNT = "early_leave_count"
        private const val TIME_ZONE = "Asia/Seoul"
    }
}

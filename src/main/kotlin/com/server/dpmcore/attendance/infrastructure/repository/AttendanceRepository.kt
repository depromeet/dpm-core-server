package com.server.dpmcore.attendance.infrastructure.repository

import com.server.dpmcore.attendance.application.query.model.MemberAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.MemberDetailAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.MemberSessionAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.MyDetailAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.SessionAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.SessionDetailAttendanceQueryModel
import com.server.dpmcore.attendance.domain.model.Attendance
import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionWeekQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetDetailAttendanceBySessionQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetDetailMemberAttendancesQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMemberAttendancesQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMyAttendanceBySessionQuery
import com.server.dpmcore.attendance.domain.port.outbound.AttendancePersistencePort
import com.server.dpmcore.attendance.infrastructure.entity.AttendanceEntity
import org.jooq.DSLContext
import org.jooq.generated.tables.references.ATTENDANCES
import org.jooq.generated.tables.references.MEMBERS
import org.jooq.generated.tables.references.MEMBER_TEAMS
import org.jooq.generated.tables.references.SESSIONS
import org.jooq.generated.tables.references.TEAMS
import org.jooq.impl.DSL
import org.jooq.impl.DSL.sum
import org.jooq.impl.DSL.`when`
import org.springframework.stereotype.Repository

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
                ATTENDANCES.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
                ATTENDANCES.STATUS,
            ).from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(
                query.toCondition(myTeamNumber),
            ).orderBy(ATTENDANCES.MEMBER_ID.asc(), TEAMS.NUMBER.asc())
            .limit(PAGE_SIZE + 1)
            .fetch { record ->
                SessionAttendanceQueryModel(
                    id = record[ATTENDANCES.MEMBER_ID]!!,
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
                ATTENDANCES.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
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
            .join(MEMBER_TEAMS)
            .on(MEMBER_TEAMS.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(TEAMS)
            .on(MEMBER_TEAMS.TEAM_ID.eq(TEAMS.TEAM_ID))
            .where(
                query.toCondition(myTeamNumber),
            ).groupBy(
                ATTENDANCES.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
            ).orderBy(ATTENDANCES.MEMBER_ID.asc(), TEAMS.NUMBER.asc())
            .limit(PAGE_SIZE + 1)
            .fetch { record ->
                MemberAttendanceQueryModel(
                    id = record[ATTENDANCES.MEMBER_ID]!!,
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
            ).from(ATTENDANCES)
            .join(MEMBERS)
            .on(ATTENDANCES.MEMBER_ID.eq(MEMBERS.MEMBER_ID))
            .join(SESSIONS)
            .on(ATTENDANCES.SESSION_ID.eq(SESSIONS.SESSION_ID))
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
                SESSIONS.EVENT_NAME,
                SESSIONS.DATE,
                ATTENDANCES.STATUS,
                ATTENDANCES.ATTENDED_AT,
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
                    sessionEventName = it[SESSIONS.EVENT_NAME]!!,
                    sessionDate = it[SESSIONS.DATE]!!,
                    attendanceStatus = it[ATTENDANCES.STATUS]!!,
                    attendedAt = it[ATTENDANCES.ATTENDED_AT],
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
                )
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
            .where(query.toCondition())
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
            .where(query.toCondition())
            .fetchOne {
                MyDetailAttendanceQueryModel(
                    attendanceStatus = it[ATTENDANCES.STATUS]!!,
                    attendedAt = it[ATTENDANCES.ATTENDED_AT],
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

    companion object {
        private const val LATE_COUNT = "late_count"
        private const val ONLINE_ABSENT_COUNT = "online_absent_count"
        private const val OFFLINE_ABSENT_COUNT = "offline_absent_count"
        private const val PRESENT_COUNT = "present_count"
        private const val EXCUSED_ABSENT_COUNT = "excused_absent_count"
    }
}

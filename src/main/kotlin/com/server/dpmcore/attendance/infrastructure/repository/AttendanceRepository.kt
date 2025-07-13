package com.server.dpmcore.attendance.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.singleQuery
import com.server.dpmcore.attendance.application.query.model.MemberAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.SessionAttendanceQueryModel
import com.server.dpmcore.attendance.domain.model.Attendance
import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionIdQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMemberAttendancesQuery
import com.server.dpmcore.attendance.domain.port.outbound.AttendancePersistencePort
import com.server.dpmcore.attendance.infrastructure.entity.AttendanceEntity
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId
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
    private val queryFactory: SpringDataQueryFactory,
    private val dsl: DSLContext,
) : AttendancePersistencePort {
    override fun findAttendanceBy(
        sessionId: SessionId,
        memberId: MemberId,
    ): Attendance =
        queryFactory
            .singleQuery<AttendanceEntity> {
                select(entity(AttendanceEntity::class))
                from(entity(AttendanceEntity::class))
                whereAnd(
                    col(AttendanceEntity::sessionId).equal(sessionId.value),
                    col(AttendanceEntity::memberId).equal(memberId.value),
                )
            }.toDomain()

    override fun save(attendance: Attendance) {
        attendanceJpaRepository.save(AttendanceEntity.from(attendance))
    }

    override fun findSessionAttendencesByQuery(
        query: GetAttendancesBySessionIdQuery,
    ): List<SessionAttendanceQueryModel> =
        dsl
            .select(
                ATTENDANCES.ID,
                MEMBERS.MEMBER_ID,
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
                query.toCondition(),
            ).orderBy(ATTENDANCES.ID.asc(), TEAMS.NUMBER.asc())
            .limit(PAGE_SIZE + 1)
            .fetch { record ->
                SessionAttendanceQueryModel(
                    id = record[MEMBERS.MEMBER_ID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = record[TEAMS.NUMBER]!!,
                    part = record[MEMBERS.PART]!!,
                    attendanceStatus = record[ATTENDANCES.STATUS]!!,
                )
            }

    override fun findMemberAttendancesByQuery(query: GetMemberAttendancesQuery): List<MemberAttendanceQueryModel> =
        dsl
            .select(
                ATTENDANCES.ID,
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
                sum(
                    `when`(ATTENDANCES.STATUS.eq("LATE"), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`("late_count"),
                sum(
                    `when`(ATTENDANCES.STATUS.eq("ABSENT").and(SESSIONS.IS_ONLINE.eq(true)), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`("online_absent_count"),
                sum(
                    `when`(ATTENDANCES.STATUS.eq("ABSENT").and(SESSIONS.IS_ONLINE.eq(false)), DSL.inline(1))
                        .otherwise(DSL.inline(0)),
                ).`as`("offline_absent_count"),
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
                query.toCondition(),
            ).groupBy(
                ATTENDANCES.ID,
                MEMBERS.MEMBER_ID,
                MEMBERS.NAME,
                TEAMS.NUMBER,
                MEMBERS.PART,
            ).orderBy(ATTENDANCES.ID.asc(), TEAMS.NUMBER.asc())
            .limit(PAGE_SIZE + 1)
            .fetch { record ->
                MemberAttendanceQueryModel(
                    id = record[MEMBERS.MEMBER_ID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = record[TEAMS.NUMBER]!!,
                    part = record[MEMBERS.PART]!!,
                    lateCount = record.get("late_count", Int::class.java) ?: 0,
                    onlineAbsentCount = record.get("online_absent_count", Int::class.java) ?: 0,
                    offlineAbsentCount = record.get("offline_absent_count", Int::class.java) ?: 0,
                )
            }
}

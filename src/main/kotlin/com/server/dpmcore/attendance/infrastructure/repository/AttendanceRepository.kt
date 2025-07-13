package com.server.dpmcore.attendance.infrastructure.repository

import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.spring.data.SpringDataQueryFactory
import com.linecorp.kotlinjdsl.spring.data.singleQuery
import com.server.dpmcore.attendance.application.query.model.SessionAttendanceQueryModel
import com.server.dpmcore.attendance.domain.model.Attendance
import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionIdQuery
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
                    attendanceId = record[ATTENDANCES.ID]!!,
                    id = record[MEMBERS.MEMBER_ID]!!,
                    name = record[MEMBERS.NAME]!!,
                    teamNumber = record[TEAMS.NUMBER]!!,
                    part = record[MEMBERS.PART]!!,
                    attendanceStatus = record[ATTENDANCES.STATUS]!!,
                )
            }
}

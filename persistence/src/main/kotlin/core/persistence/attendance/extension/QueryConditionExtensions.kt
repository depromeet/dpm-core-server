package core.persistence.attendance.extension

import core.domain.attendance.port.inbound.query.GetAttendancesBySessionWeekQuery
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
import org.jooq.dsl.tables.references.ATTENDANCES
import org.jooq.dsl.tables.references.MEMBERS
import org.jooq.dsl.tables.references.SESSIONS
import org.jooq.dsl.tables.references.TEAMS
import org.jooq.Condition


fun GetAttendancesBySessionWeekQuery.toCondition(myTeamNumber: Int?): List<Condition> {
    val conditions = mutableListOf<Condition>()

    conditions += SESSIONS.SESSION_ID.eq(this.sessionId.value)

    this.statuses?.takeIf { it.isNotEmpty() }?.let {
        conditions += ATTENDANCES.STATUS.`in`(it)
    }

    when {
        this.onlyMyTeam == true ->
            conditions += TEAMS.NUMBER.eq(myTeamNumber ?: 0)

        this.teams?.isNotEmpty() == true ->
            conditions += TEAMS.NUMBER.`in`(this.teams)
    }

    this.name?.takeIf { it.isNotBlank() }?.let {
        conditions += MEMBERS.NAME.containsIgnoreCase(it)
    }

    this.cursorId?.let {
        conditions += ATTENDANCES.MEMBER_ID.greaterOrEqual(it)
    }

    return conditions
}

fun GetMemberAttendancesQuery.toCondition(myTeamNumber: Int?): List<Condition> {
    val conditions = mutableListOf<Condition>()

    this.statuses?.takeIf { it.isNotEmpty() }?.let {
        conditions += ATTENDANCES.STATUS.`in`(it)
    }

    when {
        this.onlyMyTeam == true ->
            conditions += TEAMS.NUMBER.eq(myTeamNumber ?: 0)

        this.teams?.isNotEmpty() == true ->
            conditions += TEAMS.NUMBER.`in`(teams)
    }

    this.name?.takeIf { it.isNotBlank() }?.let {
        conditions += MEMBERS.NAME.containsIgnoreCase(it)
    }

    this.cursorId?.let {
        conditions += ATTENDANCES.MEMBER_ID.greaterOrEqual(it)
    }

    return conditions
}

fun GetDetailAttendanceBySessionQuery.toCondition(): List<Condition> {
    val conditions = mutableListOf<Condition>()

    conditions += ATTENDANCES.SESSION_ID.eq(this.sessionId.value)
    conditions += ATTENDANCES.MEMBER_ID.eq(this.memberId.value)
    return conditions
}

fun GetDetailMemberAttendancesQuery.toCondition(): List<Condition> {
    val conditions = mutableListOf<Condition>()

    conditions += ATTENDANCES.MEMBER_ID.eq(this.memberId.value)

    return conditions
}

fun GetMyAttendanceBySessionQuery.toCondition(): List<Condition> {
    val conditions = mutableListOf<Condition>()

    conditions += ATTENDANCES.SESSION_ID.eq(this.sessionId.value)
    conditions += ATTENDANCES.MEMBER_ID.eq(this.memberId.value)

    return conditions
}

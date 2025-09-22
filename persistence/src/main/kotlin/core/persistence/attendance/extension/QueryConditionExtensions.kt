package core.persistence.attendance.extension

import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionWeekQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetDetailAttendanceBySessionQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetDetailMemberAttendancesQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMemberAttendancesQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMyAttendanceBySessionQuery
import org.jooq.Condition
import org.jooq.generated.tables.references.ATTENDANCES
import org.jooq.generated.tables.references.MEMBERS
import org.jooq.generated.tables.references.SESSIONS
import org.jooq.generated.tables.references.TEAMS

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

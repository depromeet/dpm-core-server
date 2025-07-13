package com.server.dpmcore.attendance.infrastructure.repository

import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionIdQuery
import org.jooq.Condition
import org.jooq.generated.tables.references.ATTENDANCES
import org.jooq.generated.tables.references.MEMBERS
import org.jooq.generated.tables.references.TEAMS

fun GetAttendancesBySessionIdQuery.toCondition(): List<Condition> {
    val conditions = mutableListOf<Condition>()

    conditions += ATTENDANCES.SESSION_ID.eq(this.sessionId.value)

    this.statuses?.takeIf { it.isNotEmpty() }?.let {
        conditions += ATTENDANCES.STATUS.`in`(it)
    }

    this.teams?.takeIf { it.isNotEmpty() }?.let {
        conditions += TEAMS.NUMBER.`in`(it)
    }

    this.name?.takeIf { it.isNotBlank() }?.let {
        conditions += MEMBERS.NAME.containsIgnoreCase(it)
    }

    this.cursorId?.let {
        conditions += ATTENDANCES.ID.gt(it)
    }

    return conditions
}

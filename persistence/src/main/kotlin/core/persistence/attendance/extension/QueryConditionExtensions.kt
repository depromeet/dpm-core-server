package core.persistence.attendance.extension

import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
import org.jooq.Condition
import org.jooq.dsl.tables.references.ATTENDANCES

fun GetDetailAttendanceBySessionQuery.toCondition(): List<Condition> {
    val conditions = mutableListOf<Condition>()

    conditions += ATTENDANCES.SESSION_ID.eq(this.sessionId.value)
    conditions += ATTENDANCES.MEMBER_ID.eq(this.memberId.value)
    conditions += ATTENDANCES.DELETED_AT.isNull
    return conditions
}

fun GetDetailMemberAttendancesQuery.toCondition(): List<Condition> {
    val conditions = mutableListOf<Condition>()

    conditions += ATTENDANCES.MEMBER_ID.eq(this.memberId.value)
    conditions += ATTENDANCES.DELETED_AT.isNull

    return conditions
}

fun GetMyAttendanceBySessionQuery.toCondition(): List<Condition> {
    val conditions = mutableListOf<Condition>()

    conditions += ATTENDANCES.SESSION_ID.eq(this.sessionId.value)
    conditions += ATTENDANCES.MEMBER_ID.eq(this.memberId.value)
    conditions += ATTENDANCES.DELETED_AT.isNull

    return conditions
}

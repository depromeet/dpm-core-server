package core.domain.attendance.port.outbound

import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.port.inbound.query.GetAttendancesBySessionWeekQuery
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
import core.domain.attendance.port.outbound.query.MemberAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberSessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MyDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionDetailAttendanceQueryModel

interface AttendancePersistencePort {
    fun findAttendanceBy(
        sessionId: Long,
        memberId: Long,
    ): Attendance?

    fun save(attendance: Attendance)

    fun findSessionAttendancesByQuery(
        query: GetAttendancesBySessionWeekQuery,
        myTeamNumber: Int?,
    ): List<SessionAttendanceQueryModel>

    fun findMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: Int?,
    ): List<MemberAttendanceQueryModel>

    fun findDetailAttendanceBySession(query: GetDetailAttendanceBySessionQuery): SessionDetailAttendanceQueryModel?

    fun findDetailMemberAttendance(query: GetDetailMemberAttendancesQuery): MemberDetailAttendanceQueryModel?

    fun findMemberSessionAttendances(query: GetDetailMemberAttendancesQuery): List<MemberSessionAttendanceQueryModel>

    fun findMyDetailAttendanceBySession(query: GetMyAttendanceBySessionQuery): MyDetailAttendanceQueryModel?

    fun saveInBatch(attendances: List<Attendance>)

    fun countSessionAttendancesByQuery(
        query: GetAttendancesBySessionWeekQuery,
        myTeamNumber: Int?,
    ): Int

    fun countMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: Int?,
    ): Int
}

package core.domain.attendance.port.outbound

import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
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
import core.domain.attendance.port.outbound.query.SessionRosterQueryModel
import core.domain.team.vo.TeamNumber
import java.time.Instant

interface AttendancePersistencePort {
    fun findAttendanceBy(
        sessionId: Long,
        memberId: Long,
    ): Attendance?

    fun save(attendance: Attendance)

    fun findSessionAttendancesByQuery(
        query: GetAttendancesBySessionWeekQuery,
        myTeamNumber: TeamNumber,
    ): List<SessionAttendanceQueryModel>

    fun findMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: TeamNumber,
    ): List<MemberAttendanceQueryModel>

    /**
     * 세션의 전체 출석 명단. [cohortId] 기수에 소속되고 삭제되지 않은 멤버의 살아 있는 출석 기록만, 멤버당 한 행으로 준다.
     * 한 멤버에 살아 있는 기록이 여러 개면 attendance_id 가 가장 큰 것을 쓴다.
     * 팀 번호 오름차순(팀 없음은 마지막), 이름, 멤버 ID 순이다.
     */
    fun findSessionRoster(
        sessionId: Long,
        cohortId: Long,
    ): List<SessionRosterQueryModel>

    /** [cohortId] 기수에서 멤버에게 가장 최근 배정된 팀 번호. 없으면 null */
    fun findTeamNumberInCohort(
        memberId: Long,
        cohortId: Long,
    ): Int?

    fun findDetailAttendanceBySession(query: GetDetailAttendanceBySessionQuery): SessionDetailAttendanceQueryModel?

    fun findDetailMemberAttendance(query: GetDetailMemberAttendancesQuery): List<MemberDetailAttendanceQueryModel>

    fun findMemberSessionAttendances(query: GetDetailMemberAttendancesQuery): List<MemberSessionAttendanceQueryModel>

    fun findMyDetailAttendanceBySession(query: GetMyAttendanceBySessionQuery): MyDetailAttendanceQueryModel?

    fun saveInBatch(attendances: List<Attendance>)

    fun countSessionAttendancesByQuery(
        query: GetAttendancesBySessionWeekQuery,
        myTeamNumber: TeamNumber,
    ): Int

    fun countMemberAttendancesByQuery(
        query: GetMemberAttendancesQuery,
        myTeamNumber: TeamNumber,
    ): Int

    fun findAllBySessionId(sessionId: Long): List<Attendance>

    // 아래 쓰기는 모두 조건부 단일 UPDATE 라 다른 쓰기의 결과를 덮어쓰지 않는다. 호출 측은 세션 행 잠금을 먼저 잡는다.

    /**
     * 운영진 변경과 인증 기록이 없는 PENDING 또는 자동 결석(표지 있음) 행만 갱신하고 표지를 지운다.
     * 표지 없는 기존 ABSENT 는 덮어쓰지 않는다.
     */
    fun recordAttendanceIfAllowed(
        attendanceId: Long,
        status: AttendanceStatus,
        attendedAt: Instant,
    ): Boolean

    /** 상태와 updatedAt 을 바꾸고 자동 결석 표지를 지운다. attendedAt 은 보존한다. 갱신한 행 수를 반환한다. */
    fun updateStatusByAdmin(
        sessionId: Long,
        memberIds: List<Long>,
        status: AttendanceStatus,
        updatedAt: Instant,
    ): Int

    /** 지정한 멤버들 중 삭제되지 않은 출석 행이 있는 멤버 수 */
    fun countActiveAttendances(
        sessionId: Long,
        memberIds: List<Long>,
    ): Int

    /** 상태가 [expectedStatus] 이고 인증 기록이 있으며 운영진 변경이 없는 행만 바꾼다. updatedAt 은 남기지 않는다. */
    fun updateStatusByPolicy(
        attendanceId: Long,
        expectedStatus: AttendanceStatus,
        newStatus: AttendanceStatus,
    ): Boolean

    /** 마감 연장 시 자동 결석(표지 있음)만 PENDING 으로 되돌린다. 표지 없는 기존 ABSENT 는 바꾸지 않는다. */
    fun reopenAutoAbsence(attendanceId: Long): Boolean

    /**
     * 현재 PENDING 인 행만 ABSENT 로 바꾸고 자동 결석 표지를 남긴다. attendedAt/updatedAt 은 조건으로 보지 않고 그대로 둔다.
     * 운영진이 PENDING 으로 되돌린 행도 대상이다. updatedAt 은 기록하지 않는다.
     */
    fun markAutoAbsence(
        sessionId: Long,
        autoAbsentAt: Instant,
    ): Int

    fun softDeleteAllBySessionId(
        sessionId: Long,
        deletedAt: Instant,
    ): Int
}

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

    /*
     * 아래 쓰기 메서드는 모두 DB 에서 조건을 확인하는 단일 UPDATE 문입니다.
     * 읽은 엔티티 전체를 다시 저장하지 않으므로 다른 쓰기(인증/운영진 변경/정책 재계산/삭제)의 결과를 덮어쓰지 않습니다.
     * 호출하는 트랜잭션은 세션 행 잠금을 먼저 잡은 뒤 출석 행을 갱신합니다(잠금 순서: session -> attendance).
     */

    /**
     * 인증 결과를 기록합니다. 삭제되지 않았고, 운영진 변경과 인증 기록이 없는 PENDING 행만 갱신합니다.
     * updatedAt 은 기록하지 않습니다.
     *
     * @return 실제로 기록했으면 true
     */
    fun recordAttendanceIfAllowed(
        attendanceId: Long,
        status: AttendanceStatus,
        attendedAt: Instant,
    ): Boolean

    /**
     * 운영진 변경. 지정한 멤버들의 삭제되지 않은 출석 행의 상태와 updatedAt 을 바꿉니다(attendedAt 보존).
     *
     * @return 갱신한 행 수
     */
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

    /**
     * 세션 정책 재계산으로 인증 기록이 있는 행을 다시 판정합니다. 상태가 [expectedStatus] 이고 attendedAt 이 있으며
     * 운영진 변경이 없는 삭제되지 않은 행만 바꿉니다. updatedAt 은 기록하지 않습니다(재계산을 운영진 변경으로 오인하지 않도록).
     */
    fun updateStatusByPolicy(
        attendanceId: Long,
        expectedStatus: AttendanceStatus,
        newStatus: AttendanceStatus,
    ): Boolean

    /** 세션의 삭제되지 않은 출석 행을 소프트 삭제합니다. 이미 삭제된 행은 건드리지 않습니다. */
    fun softDeleteAllBySessionId(
        sessionId: Long,
        deletedAt: Instant,
    ): Int
}

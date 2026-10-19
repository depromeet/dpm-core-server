package core.application.attendance.application.service

import core.application.attendance.application.exception.AttendanceNotFoundException
import core.application.attendance.presentation.mapper.AttendanceMapper
import core.application.attendance.presentation.response.DetailAttendancesBySessionResponse
import core.application.attendance.presentation.response.DetailMemberAttendancesResponse
import core.application.attendance.presentation.response.MemberAttendancesResponse
import core.application.attendance.presentation.response.MyDetailAttendanceBySessionResponse
import core.application.attendance.presentation.response.SessionRosterResponse
import core.application.session.application.exception.SessionNotFoundException
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AttendanceQueryService(
    private val attendancePersistencePort: AttendancePersistencePort,
    private val attendanceGraduationEvaluator: AttendanceGraduationEvaluator,
    private val cohortQueryUseCase: CohortQueryUseCase,
    private val sessionPersistencePort: SessionPersistencePort,
) {
    /**
     * 현재 활성 기수 세션의 전체 출석 명단. 필터와 페이지 없이 준다.
     * 없거나 삭제됐거나 다른 기수의 세션이면 404 다. (SessionQueryService 가 이 서비스를 의존하므로 세션은 포트로 읽는다.)
     */
    fun getSessionRoster(
        sessionId: SessionId,
        memberId: MemberId,
    ): SessionRosterResponse {
        val cohortId = getActiveCohortIdOf(sessionId)

        val members = attendancePersistencePort.findSessionRoster(sessionId.value, cohortId.value)
        val myTeamNumber = attendancePersistencePort.findTeamNumberInCohort(memberId.value, cohortId.value)

        return AttendanceMapper.toSessionRosterResponse(members, myTeamNumber)
    }

    /** 세션이 현재 활성 기수의 삭제되지 않은 세션이면 그 기수 ID, 아니면 404 */
    private fun getActiveCohortIdOf(sessionId: SessionId): CohortId {
        val cohortId = cohortQueryUseCase.getLatestCohortId()
        val session = sessionPersistencePort.findSessionById(sessionId.value)
        if (session == null || session.cohortId != cohortId) throw SessionNotFoundException()
        return cohortId
    }

    /**
     * 현재 활성 기수 소속 멤버 전원(출석 기록이 없어도 포함)의 수료 판정. 페이지 없이 주고 팀 필터만 받는다.
     * 정렬(팀, 이름, ID)은 조회 결과 그대로다. totalElements 는 팀 필터와 무관한 기수 전체 소속 멤버 수다.
     */
    fun getMemberAttendances(query: GetMemberAttendancesQuery): MemberAttendancesResponse {
        val cohortId = cohortQueryUseCase.getActiveCohortId().value

        val members = attendancePersistencePort.findMemberAttendances(cohortId, query.teams.orEmpty())
        val myTeamNumber = attendancePersistencePort.findTeamNumberInCohort(query.memberId.value, cohortId)
        val totalElements = attendancePersistencePort.countCohortMembers(cohortId)

        val memberResponses =
            members.map { member ->
                AttendanceMapper.toMemberAttendanceResponse(
                    member,
                    evaluation = attendanceGraduationEvaluator.evaluate(member.summary).name,
                )
            }

        return AttendanceMapper.toMemberAttendancesResponse(memberResponses, myTeamNumber, totalElements)
    }

    fun getDetailAttendanceBySession(query: GetDetailAttendanceBySessionQuery): DetailAttendancesBySessionResponse {
        val queryResult = (
            attendancePersistencePort
                .findDetailAttendanceBySession(query)
                ?: throw AttendanceNotFoundException()
        )

        return AttendanceMapper.toDetailAttendanceBySessionResponse(
            queryResult,
            evaluation = attendanceGraduationEvaluator.evaluate(queryResult.summary).name,
        )
    }

    /**
     * 현재 활성 기수 기준 사람별 출석 상세. 출석 기록이 없는 소속 멤버는 집계 0, 세션 없이 준다.
     * 없거나 삭제됐거나 현재 기수 소속이 아닌 멤버면 404 다.
     */
    fun getDetailMemberAttendances(query: GetDetailMemberAttendancesQuery): DetailMemberAttendancesResponse {
        val cohortId = cohortQueryUseCase.getActiveCohortId().value
        val memberId = query.memberId.value

        val memberAttendanceQueryResult =
            attendancePersistencePort.findDetailMemberAttendance(memberId, cohortId)
                ?: throw AttendanceNotFoundException()

        val sessionAttendanceQueryResult =
            attendancePersistencePort.findMemberSessionAttendances(memberId, cohortId)

        return AttendanceMapper.toDetailMemberAttendancesResponse(
            memberAttendanceModel = memberAttendanceQueryResult,
            sessionAttendancesModel = sessionAttendanceQueryResult,
            evaluation = attendanceGraduationEvaluator.evaluate(memberAttendanceQueryResult.summary).name,
        )
    }

    fun getMyDetailAttendanceBySession(query: GetMyAttendanceBySessionQuery): MyDetailAttendanceBySessionResponse {
        val myAttendanceQueryResult =
            attendancePersistencePort
                .findMyDetailAttendanceBySession(query)
                ?: throw AttendanceNotFoundException()

        return AttendanceMapper.toMyDetailAttendanceBySessionResponse(myAttendanceQueryResult)
    }

    fun getAttendancesBySessionId(sessionId: SessionId): List<Attendance> =
        attendancePersistencePort.findAllBySessionId(sessionId.value)

    fun getAttendancesBy(
        sessionId: SessionId,
        memberId: MemberId,
    ): Attendance =
        attendancePersistencePort.findAttendanceBy(sessionId.value, memberId.value)
            ?: throw AttendanceNotFoundException()
}

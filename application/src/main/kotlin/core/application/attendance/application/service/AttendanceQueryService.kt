package core.application.attendance.application.service

import core.application.attendance.application.exception.AttendanceNotFoundException
import core.application.attendance.application.exception.MemberAttendanceAmbiguousException
import core.application.attendance.presentation.mapper.AttendanceMapper
import core.application.attendance.presentation.response.DetailAttendancesBySessionResponse
import core.application.attendance.presentation.response.DetailMemberAttendancesResponse
import core.application.attendance.presentation.response.MemberAttendanceResponse
import core.application.attendance.presentation.response.MemberAttendancesResponse
import core.application.attendance.presentation.response.MyDetailAttendanceBySessionResponse
import core.application.attendance.presentation.response.SessionAttendancesResponse
import core.application.attendance.presentation.response.SessionRosterResponse
import core.application.member.application.service.MemberQueryService
import core.application.session.application.exception.SessionNotFoundException
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.port.inbound.query.GetAttendancesBySessionWeekQuery
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
import core.domain.team.vo.TeamNumber
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

@Service
@Transactional(readOnly = true)
class AttendanceQueryService(
    private val memberQueryService: MemberQueryService,
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

    fun getAttendancesBySession(query: GetAttendancesBySessionWeekQuery): SessionAttendancesResponse {
        val myTeamNumber: TeamNumber =
            query.onlyMyTeam
                ?.let { memberQueryService.getMemberTeamNumber(query.memberId) } ?: TeamNumber.defaultValue()

        val queryResult =
            attendancePersistencePort
                .findSessionAttendancesByQuery(query, myTeamNumber)

        val totalElements =
            attendancePersistencePort.countSessionAttendancesByQuery(query, myTeamNumber)

        val totalPages =
            ceil(totalElements / query.size.toDouble()).toInt()

        val hasNext = query.page < totalPages

        return AttendanceMapper.toSessionAttendancesResponse(
            members = queryResult,
            onlyMyTeam = query.onlyMyTeam ?: false,
            myTeamNumber = myTeamNumber,
            hasNext = hasNext,
            totalElements = totalElements,
        )
    }

    fun getMemberAttendances(query: GetMemberAttendancesQuery): MemberAttendancesResponse {
        val myTeamNumber =
            query.onlyMyTeam
                ?.let { memberQueryService.getMemberTeamNumber(query.memberId) } ?: TeamNumber.defaultValue()

        val queryResult =
            attendancePersistencePort
                .findMemberAttendancesByQuery(query, myTeamNumber)
                .sortedBy { it.teamNumber.value }

        val totalElements =
            attendancePersistencePort.countMemberAttendancesByQuery(query, myTeamNumber)

        val totalPages =
            ceil(totalElements / query.size.toDouble()).toInt()

        val hasNext = query.page < totalPages

        return AttendanceMapper.toMemberAttendancesResponse(
            members =
                queryResult
                    .map { member ->
                        MemberAttendanceResponse(
                            id = member.id,
                            name = member.name,
                            teamNumber = member.teamNumber,
                            isAdmin = member.isAdmin,
                            part = member.part,
                            attendanceStatus = attendanceGraduationEvaluator.evaluate(member.summary).name,
                        )
                    }.toList(),
            onlyMyTeam = (query.teams?.contains(myTeamNumber.value) == true) || (query.onlyMyTeam ?: false),
            myTeamNumber = myTeamNumber,
            hasNext = hasNext,
            totalElements = totalElements,
        )
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

    fun getDetailMemberAttendances(query: GetDetailMemberAttendancesQuery): DetailMemberAttendancesResponse {
        val memberAttendanceQueryResults =
            attendancePersistencePort
                .findDetailMemberAttendance(query)

        val memberAttendanceQueryResult =
            when {
                memberAttendanceQueryResults.isEmpty() -> throw AttendanceNotFoundException()
                memberAttendanceQueryResults.size > 1 -> throw MemberAttendanceAmbiguousException()
                else -> memberAttendanceQueryResults.first()
            }

        val sessionAttendanceQueryResult =
            attendancePersistencePort
                .findMemberSessionAttendances(query)

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

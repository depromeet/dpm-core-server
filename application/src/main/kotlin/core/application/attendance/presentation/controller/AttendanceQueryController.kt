package core.application.attendance.presentation.controller

import core.application.attendance.application.service.AttendanceQueryService
import core.application.attendance.presentation.response.DetailAttendancesBySessionResponse
import core.application.attendance.presentation.response.DetailMemberAttendancesResponse
import core.application.attendance.presentation.response.MemberAttendancesResponse
import core.application.attendance.presentation.response.MyDetailAttendanceBySessionResponse
import core.application.attendance.presentation.response.SessionAttendancesResponse
import core.application.common.exception.CustomResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.query.GetAttendancesBySessionWeekQuery
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AttendanceQueryController(
    private val attendanceQueryService: AttendanceQueryService,
) : AttendanceQueryApi {
    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @GetMapping("/v1/sessions/{sessionId}/attendances")
    override fun getAttendancesBySessionId(
        @PathVariable sessionId: SessionId,
        @CurrentMemberId memberId: MemberId,
        @RequestParam(name = "statuses", required = false) statuses: List<AttendanceStatus>?,
        @RequestParam(name = "teams", required = false) teams: List<Int>?,
        @RequestParam(name = "name", required = false) name: String?,
        @RequestParam(name = "onlyMyTeam", required = false) onlyMyTeam: Boolean?,
        @RequestParam(name = "page", required = false, defaultValue = "1") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int,
    ): CustomResponse<SessionAttendancesResponse> {
        val response =
            attendanceQueryService.getAttendancesBySession(
                GetAttendancesBySessionWeekQuery(
                    sessionId = sessionId,
                    memberId = memberId,
                    statuses = statuses,
                    teams = teams,
                    name = name,
                    onlyMyTeam = onlyMyTeam,
                    page = page,
                    size = size,
                ),
            )

        return CustomResponse.ok(response)
    }

    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @GetMapping("/v1/members/attendances")
    override fun getMemberAttendances(
        @CurrentMemberId memberId: MemberId,
        @RequestParam(name = "statuses", required = false) statuses: List<AttendanceStatus>?,
        @RequestParam(name = "teams", required = false) teams: List<Int>?,
        @RequestParam(name = "name", required = false) name: String?,
        @RequestParam(name = "onlyMyTeam", required = false) onlyMyTeam: Boolean?,
        @RequestParam(name = "page", required = false, defaultValue = "1") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "20") size: Int,
    ): CustomResponse<MemberAttendancesResponse> {
        val response =
            attendanceQueryService.getMemberAttendances(
                GetMemberAttendancesQuery(
                    memberId = memberId,
                    statuses = statuses,
                    teams = teams,
                    name = name,
                    onlyMyTeam = onlyMyTeam,
                    page = page,
                    size = size,
                ),
            )

        return CustomResponse.ok(response)
    }

    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @GetMapping("/v1/sessions/{sessionId}/attendances/{memberId}")
    override fun getAttendanceBySessionIdAndMemberId(
        @PathVariable sessionId: SessionId,
        @PathVariable memberId: MemberId,
    ): CustomResponse<DetailAttendancesBySessionResponse> {
        val response =
            attendanceQueryService.getDetailAttendanceBySession(
                GetDetailAttendanceBySessionQuery(
                    sessionId = sessionId,
                    memberId = memberId,
                ),
            )

        return CustomResponse.ok(response)
    }

    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/v1/sessions/{sessionId}/attendances/me")
    override fun getMyAttendanceBySessionId(
        @PathVariable sessionId: SessionId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<MyDetailAttendanceBySessionResponse> {
        val response =
            attendanceQueryService.getMyDetailAttendanceBySession(
                GetMyAttendanceBySessionQuery(
                    sessionId = sessionId,
                    memberId = memberId,
                ),
            )

        return CustomResponse.ok(response)
    }

    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @GetMapping("/v1/members/{memberId}/attendances")
    override fun getDetailMemberAttendances(
        @PathVariable memberId: MemberId,
    ): CustomResponse<DetailMemberAttendancesResponse> {
        val response =
            attendanceQueryService.getDetailMemberAttendances(
                GetDetailMemberAttendancesQuery(
                    memberId = memberId,
                ),
            )

        return CustomResponse.ok(response)
    }

    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/v1/members/me/attendances")
    override fun getMyDetailAttendances(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<DetailMemberAttendancesResponse> {
        val response =
            attendanceQueryService.getDetailMemberAttendances(
                GetDetailMemberAttendancesQuery(
                    memberId = memberId,
                ),
            )

        return CustomResponse.ok(response)
    }
}

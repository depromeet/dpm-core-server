package com.server.dpmcore.attendance.presentation

import com.server.dpmcore.attendance.application.AttendanceCommandService
import com.server.dpmcore.attendance.application.AttendanceQueryService
import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceRecordCommand
import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionWeekQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetDetailAttendanceBySessionQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetDetailMemberAttendancesQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMemberAttendancesQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMyAttendanceBySessionQuery
import com.server.dpmcore.attendance.presentation.dto.request.AttendanceRecordRequest
import com.server.dpmcore.attendance.presentation.dto.request.AttendanceStatusUpdateRequest
import com.server.dpmcore.attendance.presentation.dto.response.AttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailAttendancesBySessionResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailMemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.MyDetailAttendanceBySessionResponse
import com.server.dpmcore.attendance.presentation.dto.response.SessionAttendancesResponse
import com.server.dpmcore.attendance.presentation.mapper.AttendanceMapper.toAttendanceResponse
import com.server.dpmcore.attendance.presentation.mapper.AttendanceMapper.toAttendanceStatusUpdateCommand
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.security.annotation.CurrentMemberId
import com.server.dpmcore.session.domain.model.SessionId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class AttendanceController(
    private val attendanceQueryService: AttendanceQueryService,
    private val attendanceCommandService: AttendanceCommandService,
) : AttendanceApi {
    @PostMapping("/v1/sessions/{sessionId}/attendances")
    override fun createAttendance(
        @PathVariable sessionId: SessionId,
        @CurrentMemberId memberId: MemberId,
        @RequestBody request: AttendanceRecordRequest,
    ): CustomResponse<AttendanceResponse> {
        val attendedAt = Instant.now()
        val attendanceStatus =
            attendanceCommandService.attendSession(
                AttendanceRecordCommand(
                    sessionId = sessionId,
                    memberId = memberId,
                    attendedAt = attendedAt,
                    attendanceCode = request.attendanceCode,
                ),
            )

        return CustomResponse.ok(toAttendanceResponse(attendanceStatus, attendedAt))
    }

    @GetMapping("/v1/sessions/{week}/attendances")
    override fun getAttendancesByWeek(
        @PathVariable week: Int,
        @RequestParam(name = "statuses", required = false) statuses: List<AttendanceStatus>?,
        @RequestParam(name = "teams", required = false) teams: List<Int>?,
        @RequestParam(name = "name", required = false) name: String?,
        @RequestParam(name = "cursorId", required = false) cursorId: Long?,
    ): CustomResponse<SessionAttendancesResponse> {
        val response =
            attendanceQueryService.getAttendancesBySession(
                GetAttendancesBySessionWeekQuery(
                    week = week,
                    statuses = statuses,
                    teams = teams,
                    name = name,
                    cursorId = cursorId,
                ),
            )

        return CustomResponse.ok(response)
    }

    @GetMapping("/v1/members/attendances")
    override fun getMemberAttendances(
        @RequestParam(name = "statuses", required = false) statuses: List<AttendanceStatus>?,
        @RequestParam(name = "teams", required = false) teams: List<Int>?,
        @RequestParam(name = "name", required = false) name: String?,
        @RequestParam(name = "cursorId", required = false) cursorId: Long?,
    ): CustomResponse<MemberAttendancesResponse> {
        val response =
            attendanceQueryService.getMemberAttendances(
                GetMemberAttendancesQuery(
                    statuses = statuses,
                    teams = teams,
                    name = name,
                    cursorId = cursorId,
                ),
            )

        return CustomResponse.ok(response)
    }

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

    @PatchMapping("/v1/sessions/{sessionId}/attendances/{memberId}")
    override fun updateAttendance(
        @PathVariable sessionId: SessionId,
        @PathVariable memberId: MemberId,
        @RequestBody request: AttendanceStatusUpdateRequest,
    ): CustomResponse<Void> {
        attendanceCommandService.updateAttendanceStatus(
            toAttendanceStatusUpdateCommand(sessionId, memberId, request),
        )

        return CustomResponse.noContent()
    }
}

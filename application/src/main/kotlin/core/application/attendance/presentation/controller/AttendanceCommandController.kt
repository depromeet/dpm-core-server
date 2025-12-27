package core.application.attendance.presentation.controller

import core.application.attendance.application.service.AttendanceCommandService
import core.application.attendance.presentation.mapper.AttendanceMapper.toAttendanceResponse
import core.application.attendance.presentation.mapper.AttendanceMapper.toAttendanceStatusUpdateCommand
import core.application.attendance.presentation.request.AttendanceRecordRequest
import core.application.attendance.presentation.request.AttendanceStatusBulkUpdateRequest
import core.application.attendance.presentation.request.AttendanceStatusUpdateRequest
import core.application.attendance.presentation.response.AttendanceResponse
import core.application.common.exception.CustomResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceRecordCommand
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class AttendanceCommandController(
    private val attendanceCommandService: AttendanceCommandService,
) : AttendanceCommandApi {
    @PreAuthorize("hasAuthority('create:attendance')")
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

    @PreAuthorize("hasAuthority('update:attendance')")
    @PatchMapping("/v1/sessions/{sessionId}/attendances/{memberId}")
    override fun updateAttendance(
        @PathVariable sessionId: SessionId,
        @PathVariable memberId: MemberId,
        @RequestBody request: AttendanceStatusUpdateRequest,
    ): CustomResponse<Void> {
        attendanceCommandService.updateAttendanceStatus(
            toAttendanceStatusUpdateCommand(sessionId, memberId, request),
        )

        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('update:attendance')")
    @PatchMapping("/v1/sessions/{sessionId}/attendances/bulk")
    override fun updateAttendanceBulk(
        @PathVariable sessionId: SessionId,
        @Valid @RequestBody request: AttendanceStatusBulkUpdateRequest,
    ): CustomResponse<Void> {
        attendanceCommandService.updateAttendanceStatusBulk(
            sessionId,
            AttendanceStatus.valueOf(request.attendanceStatus),
            request.toMemberIds(),
        )

        return CustomResponse.ok()
    }
}

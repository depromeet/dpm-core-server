package com.server.dpmcore.attendance.presentation

import com.server.dpmcore.attendance.presentation.dto.request.AttendanceCreateRequest
import com.server.dpmcore.attendance.presentation.dto.response.AttendanceResponse
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.session.domain.model.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "출석(Attendance)")
interface AttendanceApi {
    @Operation(
        summary = "세션 출석",
        description = "세션에 대한 출석을 합니다. 요청 시 현재 시간을 기준으로 미리 생성된 출석 기록의 상태를 변경합니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "출석 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "출석 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "attendanceStatus": "PRESENT",
                                            "attendedAt": "2025-08-02T14:00:00"
                                        }
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createAttendance(
        sessionId: SessionId,
        request: AttendanceCreateRequest,
    ): CustomResponse<AttendanceResponse>
}

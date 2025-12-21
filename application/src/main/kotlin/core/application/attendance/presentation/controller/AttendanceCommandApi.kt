package core.application.attendance.presentation.controller

import core.application.attendance.presentation.request.AttendanceRecordRequest
import core.application.attendance.presentation.request.AttendanceStatusBulkUpdateRequest
import core.application.attendance.presentation.request.AttendanceStatusUpdateRequest
import core.application.attendance.presentation.response.AttendanceResponse
import core.application.common.exception.CustomResponse
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Attendance Mutation", description = "출석 추가/변경 API")
interface AttendanceCommandApi {
    @Operation(
        summary = "세션 출석",
        description = "세션에 대한 출석을 합니다. 요청 시 현재 시간을 기준으로 미리 생성된 출석 기록의 상태를 변경합니다",
        requestBody =
            RequestBody(
                description = "출석 생성 요청",
                required = true,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AttendanceRecordRequest::class),
                        examples = [
                            ExampleObject(
                                name = "출석 생성 요청 예시",
                                value = """
                                {
                                    "attendanceCode": "3824"
                                }
                            """,
                            ),
                        ],
                    ),
                ],
            ),
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
                                            "attendedAt": "2025-08-02T14:00:00.000000"
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
        memberId: MemberId,
        request: AttendanceRecordRequest,
    ): CustomResponse<AttendanceResponse>

    @Operation(
        summary = "출석 상태 갱신",
        description = "출석 상태를 갱신합니다.",
        requestBody =
            RequestBody(
                description = "출석 상태 갱신 요청",
                required = true,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AttendanceStatusUpdateRequest::class),
                        examples = [
                            ExampleObject(
                                name = "출석 상태 갱신 요청 예시",
                                value = """
                                    {
                                        "attendanceStatus": "LATE"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "출석 상태 갱신 성공",
            ),
        ],
    )
    fun updateAttendance(
        sessionId: SessionId,
        memberId: MemberId,
        request: AttendanceStatusUpdateRequest,
    ): CustomResponse<Void>

    @Operation(
        summary = "출석 상태 일괄 갱신",
        description = "여러 명 멤버의 출석 상태를 한 번에 갱신합니다.",
        requestBody =
            RequestBody(
                description = "출석 상태 일괄 갱신 요청",
                required = true,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AttendanceStatusBulkUpdateRequest::class),
                        examples = [
                            ExampleObject(
                                name = "출석 상태 일괄 갱신 요청 예시",
                                value = """
                                    {
                                        "attendanceStatus": "LATE",
                                        "memberIds": [1, 2, 3, 4, 5]
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "출석 상태 일괄 갱신 성공",
            ),
        ],
    )
    fun updateAttendanceBulk(
        sessionId: SessionId,
        request: AttendanceStatusBulkUpdateRequest,
    ): CustomResponse<Void>
}

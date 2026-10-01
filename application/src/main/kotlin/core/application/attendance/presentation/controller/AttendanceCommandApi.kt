package core.application.attendance.presentation.controller

import core.application.attendance.presentation.request.AbsenceReasonReviewRequest
import core.application.attendance.presentation.request.AbsenceReportCreateRequest
import core.application.attendance.presentation.request.AbsenceReportUpdateRequest
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
        description =
            "세션에 대한 출석을 합니다. 서버가 요청을 받은 시각 t 를 기준으로 미리 생성된 출석 기록의 상태를 변경합니다. " +
                "t < 인증 시작이면 SESSION-400-03(너무 이름), 인증 시작 <= t < 지각 시작이면 PRESENT, " +
                "지각 시작 <= t < 인증 마감이면 LATE, 인증 마감 <= t 이면 SESSION-400-06(마감)이며 저장하지 않습니다. " +
                "코드 불일치(SESSION-400-02)도 저장하지 않습니다. 이미 인증한 경우 SESSION-400-04, " +
                "운영진이 상태를 확정한 경우 SESSION-400-07 입니다. " +
                "마감 전에 접수된 요청은 자동 결석 처리보다 늦게 저장되더라도 정상 판정으로 저장됩니다.",
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
            ApiResponse(
                responseCode = "400",
                description =
                    "SESSION-400-02 코드 불일치, SESSION-400-03 너무 이름, SESSION-400-04 이미 출석, " +
                        "SESSION-400-06 인증 마감, SESSION-400-07 운영진이 상태 확정",
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

    @Operation(
        summary = "결석 사유 제출",
        description = "결석 사유를 제출합니다.",
        requestBody =
            RequestBody(
                description = "결석 사유 제출 요청",
                required = true,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AbsenceReportCreateRequest::class),
                        examples = [
                            ExampleObject(
                                name = "결석 사유 제출 요청 예시",
                                value = """
                                    {
                                        "contents": "아파서 병원다녀옴"
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
                description = "결석 사유 제출 성공",
            ),
        ],
    )
    fun createAbsenceReport(
        sessionId: SessionId,
        memberId: MemberId,
        request: AbsenceReportCreateRequest,
    ): CustomResponse<Void>

    @Operation(
        summary = "결석 사유 수정",
        description = "본인이 제출한 결석 사유서의 내용을 수정합니다. 수정 시 검토 상태는 다시 대기(PENDING)로 전환됩니다.",
        requestBody =
            RequestBody(
                description = "결석 사유 수정 요청",
                required = true,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AbsenceReportUpdateRequest::class),
                        examples = [
                            ExampleObject(
                                name = "결석 사유 수정 요청 예시",
                                value = """
                                    {
                                        "contents": "갑자기 일이 생겨 불참"
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
                description = "결석 사유 수정 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "제출한 결석 사유서가 존재하지 않음",
            ),
        ],
    )
    fun updateAbsenceReport(
        sessionId: SessionId,
        memberId: MemberId,
        request: AbsenceReportUpdateRequest,
    ): CustomResponse<Void>

    @Operation(
        summary = "결석 사유 삭제",
        description = "본인이 제출한 결석 사유서를 삭제합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "결석 사유 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "제출한 결석 사유서가 존재하지 않음",
            ),
        ],
    )
    fun deleteAbsenceReport(
        sessionId: SessionId,
        memberId: MemberId,
    ): CustomResponse<Void>

    @Operation(
        summary = "결석 사유 검토 (운영진)",
        description = "운영진이 제출된 결석 사유서를 승인하거나 반려합니다. 승인 시 해당 멤버의 출석 상태가 인정결석으로 변경됩니다.",
        requestBody =
            RequestBody(
                description = "결석 사유 검토 요청",
                required = true,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AbsenceReasonReviewRequest::class),
                        examples = [
                            ExampleObject(
                                name = "결석 사유 승인 요청 예시",
                                value = """
                                    {
                                        "approved": true
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
                description = "결석 사유 검토 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "제출된 결석 사유서가 존재하지 않음",
            ),
        ],
    )
    fun reviewAbsenceReport(
        sessionId: SessionId,
        memberId: MemberId,
        request: AbsenceReasonReviewRequest,
    ): CustomResponse<Void>
}

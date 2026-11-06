package core.application.attendance.presentation.controller

import core.application.attendance.presentation.request.AbsenceReasonReviewRequest
import core.application.attendance.presentation.request.AbsenceReportCreateRequest
import core.application.attendance.presentation.request.AbsenceReportUpdateRequest
import core.application.attendance.presentation.request.AttendanceRecordRequest
import core.application.attendance.presentation.request.AttendanceStatusBulkUpdateRequest
import core.application.attendance.presentation.request.AttendanceStatusUpdateRequest
import core.application.attendance.presentation.response.AttendanceRecordSuccessResponse
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
            "서버가 요청을 받은 시각으로 출석을 판정합니다. 인증 시작부터 PRESENT, 지각 시작부터 LATE 이며 " +
                "지각 인정 마감부터 세션 당일 19:00(KST) 직전까지 유효한 코드를 입력하면 ABSENT로 집계하고 피드백 참여 자격을 부여합니다. " +
                "마감 전에 접수된 요청은 자동 결석보다 늦게 저장돼도 정상 판정으로 저장됩니다. " +
                "운영진(ORGANIZER/CORE/MASTER)은 출석 인증을 할 수 없습니다(ATTENDANCE-403-01).",
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
                description = "ABSENT도 등록 성공입니다. 결석 횟수에는 포함되지만 피드백을 작성할 수 있습니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AttendanceRecordSuccessResponse::class),
                        examples = [
                            ExampleObject(
                                name = "지각 마감 후 참여 등록 성공",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "GLOBAL-200-01",
                                        "data": {
                                            "attendanceStatus": "ABSENT",
                                            "attendedAt": "2026-11-07T18:59:59"
                                        }
                                    }
                                """,
                            ),
                            ExampleObject(
                                name = "출석 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "GLOBAL-200-01",
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
            ApiResponse(
                responseCode = "403",
                description = "ATTENDANCE-403-01 운영진은 출석 인증 불가",
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
        description =
            "출석 상태를 갱신합니다. " +
                "attendanceStatus 는 PENDING, PRESENT, LATE, ABSENT, EXCUSED_ABSENT 중 하나이며 " +
                "그 밖의 값은 400(GLOBAL-400-01)입니다. " +
                "저장된 출석 인증 시각은 지우지 않으며 명단에서는 isManuallyUpdated 로 표시됩니다.",
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
        description =
            "여러 명 멤버의 출석 상태를 한 번에 갱신합니다. " +
                "attendanceStatus 는 PENDING, PRESENT, LATE, ABSENT, EXCUSED_ABSENT 중 하나이며 " +
                "그 밖의 값은 400(GLOBAL-400-01)입니다. " +
                "대상 중 하나라도 출석 기록이 없으면 아무것도 바꾸지 않습니다.",
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
        description =
            "결석 사유서를 검토 대기(PENDING)로 제출합니다. 출석 상태는 유지됩니다. " +
                "검토 대기·반려 사유서는 재제출할 수 있습니다. " +
                "처음 제출하거나 반려 후 다시 제출하면 해당 기수 운영진과 코어에게 ABSENCE_REASON_SUBMITTED 푸시를 보냅니다. " +
                "푸시 데이터의 sessionId와 memberId로 사유서를 찾을 수 있습니다. 검토 대기 중 내용을 고칠 때는 다시 보내지 않습니다. " +
                ABSENCE_REASON_IMAGE_FLOW,
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
                                name = "이미지 첨부",
                                value = """
                                    {
                                        "contents": "아파서 병원다녀옴",
                                        "imageIds": [12, 15]
                                    }
                                """,
                            ),
                            ExampleObject(
                                name = "이미지 없이(기존 클라이언트, 기존 첨부 유지)",
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
            ApiResponse(responseCode = "400", description = ABSENCE_REASON_400_DESCRIPTION),
            ApiResponse(responseCode = "404", description = "SESSION-404-01 세션 없음"),
            ApiResponse(
                responseCode = "409",
                description = "ATTENDANCE-409-02 다른 사유서에 첨부된 이미지, ATTENDANCE-409-03 이미 승인된 사유서",
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
        description =
            "본인의 검토 대기·반려 사유서를 수정하면 검토 대기(PENDING)가 됩니다. 출석 상태는 유지됩니다. " +
                ABSENCE_REASON_IMAGE_FLOW,
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
                                name = "첨부 순서 변경/교체",
                                value = """
                                    {
                                        "contents": "갑자기 일이 생겨 불참",
                                        "imageIds": [15, 12]
                                    }
                                """,
                            ),
                            ExampleObject(
                                name = "첨부 모두 해제",
                                value = """
                                    {
                                        "contents": "갑자기 일이 생겨 불참",
                                        "imageIds": []
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
            ApiResponse(responseCode = "400", description = ABSENCE_REASON_400_DESCRIPTION),
            ApiResponse(
                responseCode = "404",
                description = "SESSION-404-01 세션 없음(삭제된 세션 포함), ATTENDANCE-404-02 제출한 결석 사유서가 존재하지 않음",
            ),
            ApiResponse(
                responseCode = "409",
                description = "ATTENDANCE-409-02 다른 사유서에 첨부된 이미지, ATTENDANCE-409-03 이미 승인된 사유서",
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
        description =
            "본인의 검토 대기·반려 사유서를 삭제합니다. 출석 상태는 유지됩니다. " +
                "첨부만 해제되며 이미지는 남아 다른 사유서에 다시 첨부할 수 있습니다.",
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
            ApiResponse(responseCode = "409", description = "ATTENDANCE-409-03 이미 승인된 사유서"),
        ],
    )
    fun deleteAbsenceReport(
        sessionId: SessionId,
        memberId: MemberId,
    ): CustomResponse<Void>

    @Operation(
        summary = "결석 사유 검토 (운영진)",
        description =
            "운영진이 검토 대기(PENDING) 사유서를 승인하거나 반려합니다. 같은 결정을 반복하면 기존 결과를 반환합니다. " +
                "승인 버튼은 approved=true, 반려 버튼은 approved=false로 이 API를 호출하세요. " +
                "승인하면 인정결석이 됩니다. 반려하면 기존 출석 상태를 유지하고 디퍼가 사유서를 다시 제출할 수 있습니다. " +
                "출석 수동 변경 API만 호출하면 사유서는 검토 대기 상태로 남습니다.",
        requestBody =
            RequestBody(
                description = "심사 결과가 바뀌면 제출자에게 ABSENCE_REASON_APPROVED 또는 ABSENCE_REASON_REJECTED 푸시를 보냅니다. " +
                    "푸시 데이터에는 sessionId가 있습니다. 같은 결정을 다시 요청해도 알림은 한 번만 요청합니다.",
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
                description = "심사가 끝났습니다. 응답 본문에 심사 결과 데이터가 없으므로 사유서와 출석 정보를 다시 조회하세요.",
                useReturnTypeSchema = true,
            ),
            ApiResponse(
                responseCode = "404",
                description = "SESSION-404-01 세션 없음, ATTENDANCE-404-01 승인할 출석 기록 없음, ATTENDANCE-404-02 사유서 없음",
            ),
            ApiResponse(responseCode = "409", description = "ATTENDANCE-409-03 이전 검토 결과와 다른 결정"),
        ],
    )
    fun reviewAbsenceReport(
        sessionId: SessionId,
        memberId: MemberId,
        request: AbsenceReasonReviewRequest,
    ): CustomResponse<Void>
}

private const val ABSENCE_REASON_IMAGE_FLOW =
    "POST /v3/images/uploads → 업로드 → POST /v3/images/uploads/{uploadId}/complete 200 으로 받은 imageId 들을 " +
        "imageIds 에 표시 순서로 담습니다(규칙은 imageIds 스키마). 내용과 첨부는 함께 저장됩니다."

private const val ABSENCE_REASON_400_DESCRIPTION =
    "ATTENDANCE-400-02 사유 없음, ATTENDANCE-400-03 50자 초과, " +
        "ATTENDANCE-400-04 없거나 본인 것이 아닌 이미지/잘못된 id, ATTENDANCE-400-05 이미지 중복"

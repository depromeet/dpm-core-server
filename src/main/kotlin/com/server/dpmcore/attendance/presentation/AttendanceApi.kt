package com.server.dpmcore.attendance.presentation

import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.presentation.dto.request.AttendanceCreateRequest
import com.server.dpmcore.attendance.presentation.dto.request.AttendanceStatusUpdateRequest
import com.server.dpmcore.attendance.presentation.dto.response.AttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailAttendancesBySessionResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailMemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.SessionAttendancesResponse
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "출석(Attendance)")
interface AttendanceApi {
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
                        schema = Schema(implementation = AttendanceCreateRequest::class),
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
                                            "attendedAt": "2025-08-02 14:00:00"
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

    @Operation(
        summary = "세션별 출석 조회",
        description = "세션에 대한 출석을 조회합니다. 요청 시 출석상태, 팀, 이름, 커서 ID를 기준으로 필터링할 수 있습니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션별 출석 조회 성공",
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
                                            "members": [
                                                {
                                                    "id": 1,
                                                    "name": "신민철",
                                                    "teamNumber": 1,
                                                    "part": "SERVER",
                                                    "attendanceStatus": "PRESENT"
                                                },
                                                {
                                                    "id": 2,
                                                    "name": "이정호",
                                                    "teamNumber": 2,
                                                    "part": "WEB",
                                                    "attendanceStatus": "LATE"
                                                }
                                            ],
                                            "hasNextPage": false,
                                            "nextCursorId": null
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
    fun getAttendancesBySessionId(
        sessionId: SessionId,
        statuses: List<AttendanceStatus>?,
        teams: List<Int>?,
        name: String?,
        cursorId: Long?,
    ): CustomResponse<SessionAttendancesResponse>

    @Operation(
        summary = "사람별 출석 조회",
        description = "사람별 출석을 조회합니다. 요청 시 출석상태, 팀, 이름, 커서 ID를 기준으로 필터링할 수 있습니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사람별 출석 조회 성공",
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
                                            "members": [
                                                {
                                                    "id": 1,
                                                    "name": "신민철",
                                                    "teamNumber": 1,
                                                    "part": "SERVER",
                                                    "attendanceStatus": "AT_RISK"
                                                },
                                                {
                                                    "id": 1,
                                                    "name": "이정호",
                                                    "teamNumber": 2,
                                                    "part": "WEB",
                                                    "attendanceStatus": "NORMAL"
                                                }
                                            ],
                                            "hasNextPage": false,
                                            "nextCursorId": null
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
    fun getMemberAttendances(
        statuses: List<AttendanceStatus>?,
        teams: List<Int>?,
        name: String?,
        cursorId: Long?,
    ): CustomResponse<MemberAttendancesResponse>

    @Operation(
        summary = "세션별 개인 출석 상세 조회",
        description = "세션별 개인 출석을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션별 개인 출석 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "세션별 개인 출석 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "member": {
                                                "id": 1,
                                                "name": "신민철",
                                                "teamNumber": 2,
                                                "part": "SERVER",
                                                "attendanceStatus": "NORMAL"
                                            },
                                            "session": {
                                                "id": 1,
                                                "week": 2,
                                                "eventName": "2주차 세션",
                                                "date": "2025-08-09 14:00:00"
                                            },
                                            "attendance": {
                                                "status": "LATE",
                                                "attendedAt": "2025-08-09 14:05:12"
                                            }
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
    fun getAttendanceBySessionIdAndMemberId(
        sessionId: SessionId,
        memberId: MemberId,
    ): CustomResponse<DetailAttendancesBySessionResponse>

    @Operation(
        summary = "사람별 출석 상세 조회",
        description = "사람별 출석을 상세하게 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사람별 출석 상세 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "사람별 출석 상세 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "member": {
                                                "id": 1,
                                                "name": "신민철",
                                                "teamNumber": 2,
                                                "part": "SERVER",
                                                "attendanceStatus": "NORMAL"
                                            },
                                            "attendance": {
                                                "presentCount": 1,
                                                "lateCount": 1,
                                                "excusedAbsentCount": 0,
                                                "absentCount": 0
                                            },
                                            "sessions": [
                                                {
                                                    "id": 1,
                                                    "week": 1,
                                                    "eventName": "디프만 17기 OT",
                                                    "date": "2025-08-02 14:03:42",
                                                    "attendanceStatus": "PRESENT"
                                                },
                                                {
                                                    "id": 6,
                                                    "week": 2,
                                                    "eventName": "2주차 세션",
                                                    "date": "2025-08-09 14:09:12",
                                                    "attendanceStatus": "LATE"
                                                }
                                            ]
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
    fun getDetailMemberAttendances(memberId: MemberId): CustomResponse<DetailMemberAttendancesResponse>

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
                responseCode = "204",
                description = "출석 상태 갱신 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "출석 상태 갱신 성공 응답",
                                value = """
                                    {
                                        "status": "NO_CONTENT",
                                        "message": "요청에 성공했지만 반환할 데이터가 없습니다.",
                                        "code": "G004"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateAttendance(
        sessionId: SessionId,
        memberId: MemberId,
        request: AttendanceStatusUpdateRequest,
    ): CustomResponse<Void>
}

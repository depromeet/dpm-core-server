package core.application.attendance.presentation.controller

import core.application.attendance.presentation.request.AttendanceRecordRequest
import core.application.attendance.presentation.request.AttendanceStatusUpdateRequest
import core.application.attendance.presentation.response.AttendanceResponse
import core.application.attendance.presentation.response.DetailAttendancesBySessionResponse
import core.application.attendance.presentation.response.DetailMemberAttendancesResponse
import core.application.attendance.presentation.response.MemberAttendancesResponse
import core.application.attendance.presentation.response.MyDetailAttendanceBySessionResponse
import core.application.attendance.presentation.response.SessionAttendancesResponse
import core.application.common.exception.CustomResponse
import core.domain.attendance.enums.AttendanceStatus
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
                                            "nextCursorId": null,
                                            "totalElements": 26
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
        memberId: MemberId,
        statuses: List<AttendanceStatus>?,
        teams: List<Int>?,
        name: String?,
        onlyMyTeam: Boolean?,
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
                                            "nextCursorId": null,
                                            "totalElements": 26
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
        memberId: MemberId,
        statuses: List<AttendanceStatus>?,
        teams: List<Int>?,
        name: String?,
        onlyMyTeam: Boolean?,
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
                                                "date": "2025-08-09T14:00:00.000000"
                                            },
                                            "attendance": {
                                                "status": "LATE",
                                                "attendedAt": "2025-08-09T14:05:12.000000"
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
        summary = "세션별 나의 출석 상세 조회",
        description = "세션별 나의 출석을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션별 나의 출석 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "세션별 나의 출석 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "attendance": {
                                                "status": "PRESENT",
                                                "attendedAt": "2025-08-09T14:05:12.000000"
                                            },
                                            "session": {
                                                "week": 1,
                                                "eventName": "디프만 17기 OT",
                                                "date": "2025-08-09T14:00:00.000000",
                                                "place": "공덕"
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
    fun getMyAttendanceBySessionId(
        sessionId: SessionId,
        memberId: MemberId,
    ): CustomResponse<MyDetailAttendanceBySessionResponse>

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
                                                "absentCount": 0,
                                                "earlyLeaveCount": 0
                                            },
                                            "sessions": [
                                                {
                                                    "id": 1,
                                                    "week": 1,
                                                    "eventName": "디프만 17기 OT",
                                                    "date": "2025-08-02T14:03:42.000000",
                                                    "attendanceStatus": "PRESENT"
                                                },
                                                {
                                                    "id": 6,
                                                    "week": 2,
                                                    "eventName": "2주차 세션",
                                                    "date": "2025-08-09T14:09:12.000000",
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
        summary = "나의 출석 리스트 상세 조회",
        description = "나의 출석 리스트를 상세하게 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "나의 출석 리스트 상세 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "나의 출석 리스트 상세 조회 성공 응답",
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
                                                "absentCount": 0,
                                                "earlyLeaveCount": 0
                                            },
                                            "sessions": [
                                                {
                                                    "id": 1,
                                                    "week": 1,
                                                    "eventName": "디프만 17기 OT",
                                                    "date": "2025-08-02T14:03:42.000000",
                                                    "attendanceStatus": "PRESENT"
                                                },
                                                {
                                                    "id": 6,
                                                    "week": 2,
                                                    "eventName": "2주차 세션",
                                                    "date": "2025-08-09T14:09:12.000000",
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
    fun getMyDetailAttendances(memberId: MemberId): CustomResponse<DetailMemberAttendancesResponse>

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
                content = [],
            ),
        ],
    )
    fun updateAttendance(
        sessionId: SessionId,
        memberId: MemberId,
        request: AttendanceStatusUpdateRequest,
    ): CustomResponse<Void>
}

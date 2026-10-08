package core.application.session.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.session.presentation.response.AttendanceTimeResponse
import core.application.session.presentation.response.NextSessionResponse
import core.application.session.presentation.response.SessionDetailForDeeperResponse
import core.application.session.presentation.response.SessionDetailResponse
import core.application.session.presentation.response.SessionListResponse
import core.application.session.presentation.response.SessionPolicyUpdateTargetResponse
import core.application.session.presentation.response.SessionSelectorResponse
import core.application.session.presentation.response.SessionWeeksResponse
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDateTime

@Tag(name = "Session Query", description = "세션 조회 API")
interface SessionQueryApi {
    @Operation(
        summary = "다음 세션 조회",
        description = "현재 시간 이후의 가장 가까운 세션을 조회합니다. 만약 현재 시간이 세션이 없는 경우, data를 반환하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "다음 세션 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "다음 세션 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "GLOBAL-200-1",
                                        "data": {
                                            "id": 1,
                                            "week": 1,
                                            "name": "디프만 17기 OT",
                                            "place": "공덕 프론트원",
                                            "isOnline": false,
                                            "date": "2025-08-02T14:00:00.000000",
                                            "attendanceStart": "2025-08-02T14:00:00.000000",
                                            "lateStart": "2025-08-02T14:10:00.000000",
                                            "absentStart": "2025-08-02T14:20:00.000000",
                                            "attendanceCode": "3821"
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
    fun getNextSession(): CustomResponse<NextSessionResponse>

    @Operation(
        summary = "기수 모든 세션 조회",
        description = "기수에 속한 모든 세션을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션 목록 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "세션 목록 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "GLOBAL-200-01",
                                        "data": {
                                            "sessions": [
                                                {
                                                    "id": 1,
                                                    "week": 1,
                                                    "name": "디프만 19기 OT",
                                                    "date": "2026-10-10T14:00:00",
                                                    "place": "공덕 프론트원",
                                                    "isOnline": false,
                                                    "feedback": {
                                                        "status": "IN_PROGRESS",
                                                        "endAt": "2026-10-13T18:00:00",
                                                        "canSubmit": true
                                                    }
                                                },
                                                {
                                                    "id": 2,
                                                    "week": 2,
                                                    "name": "미니 디프콘",
                                                    "date": "2026-10-17T14:00:00",
                                                    "place": null,
                                                    "isOnline": true,
                                                    "feedback": null
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
    fun getAllSessions(): CustomResponse<SessionListResponse>

    @Operation(
        summary = "세션 상세 조회",
        description = "세션 ID를 통해 세션의 상세 정보를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션 상세 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "세션 상세 조회 성공 응답 (피드백 ON)",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "GLOBAL-200-01",
                                        "data": {
                                            "id": 1,
                                            "week": 1,
                                            "name": "디프만 19기 OT",
                                            "place": "공덕 프론트원",
                                            "isOnline": false,
                                            "date": "2026-10-10T14:00:00",
                                            "attendanceStart": "2026-10-10T14:00:00",
                                            "lateStart": "2026-10-10T14:16:00",
                                            "absentStart": "2026-10-10T14:31:00",
                                            "attendanceCode": "3821",
                                            "feedback": {
                                                "status": "SCHEDULED",
                                                "startAt": "2026-10-10T18:00:00",
                                                "endAt": "2026-10-13T18:00:00",
                                                "pushEnabled": true
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
    fun getSessionById(sessionId: SessionId): CustomResponse<SessionDetailResponse>

    @Operation(
        summary = "(디퍼)세션 상세 조회",
        description = "세션 ID를 통해 세션에 대한 디퍼의 상세 정보를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "(디퍼)세션 상세 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "(디퍼)세션 상세 조회 성공 응답",
                                value = """
                                    {
                                      "status": "OK",
                                      "message": "요청에 성공했습니다",
                                      "code": "GLOBAL-200-01",
                                      "data": {
                                        "id": 35,
                                        "week": 1,
                                        "name": "코어 OT & 팀빌딩",
                                        "place": "스터디룸",
                                        "isOnline": false,
                                        "date": "2026-03-14T13:00:00",
                                        "attendanceStart": "2026-03-14T14:00:00",
                                        "lateStart": "2026-03-14T14:30:00",
                                        "absentStart": "2026-03-14T14:35:00",
                                        "attendanceStatus": "PENDING",
                                        "attendedAt": null
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
    fun getSessionByIdForDeeper(
        sessionId: SessionId,
        memberId: MemberId,
    ): CustomResponse<SessionDetailForDeeperResponse>

    @Operation(
        summary = "세션 출석시간 조회",
        description = "세션 ID를 통해 해당 세션의 출석 시작 시간을 조회합니다. 출석 시작 시간은 세션의 출석 정책에 따라 결정됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션 출석시간 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "세션 출석시간 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "GLOBAL-200-1",
                                        "data": {
                                            "attendanceStartTime": "2025-08-02T14:00:00.000000"
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
    fun getAttendanceTime(sessionId: SessionId): CustomResponse<AttendanceTimeResponse>

    @Operation(
        summary = "세션 주차 조회",
        description = "세션 주차를 조회합니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션 주차 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "세션 주차 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "GLOBAL-200-1",
                                        "data": {
                                            "sessions": [
                                              {
                                                "id": 5,
                                                "week": 1,
                                                "date": "2025-08-02T13:00:00"
                                              },
                                              {
                                                "id": 6,
                                                "week": 2,
                                                "date": "2025-08-09T14:00:00"
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
    fun getSessionWeeks(): CustomResponse<SessionWeeksResponse>

    @Operation(
        summary = "세션 선택 목록 조회 (v3)",
        description =
            "현재 활성 기수의 세션을 세션 일시 오름차순, 같으면 세션 ID 오름차순으로 조회합니다. " +
                "week 는 표시용이며 정렬에 쓰지 않습니다. 이전/다음 세션 이동은 이 목록의 순서를 사용합니다. " +
                "attendanceStatus 는 서버 현재 시각 기준 출석 인증 상태로, 출석 시작 전 NOT_STARTED, " +
                "출석 시작부터 인증 마감(결석 시작) 전까지 IN_PROGRESS(지각 구간 포함), 마감 정각부터 CLOSED 입니다. " +
                "기존 GET /v1/sessions/weeks(id, week, date, ID 순)는 그대로 둡니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션 선택 목록 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "세션 선택 목록 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "GLOBAL-200-01",
                                        "data": {
                                            "sessions": [
                                              {
                                                "id": 5,
                                                "week": 1,
                                                "date": "2025-08-02T13:00:00",
                                                "eventName": "OT",
                                                "place": "디프만 오프라인 장소",
                                                "isOnline": false,
                                                "attendanceStatus": "CLOSED"
                                              },
                                              {
                                                "id": 6,
                                                "week": 2,
                                                "date": "2025-08-09T14:00:00",
                                                "eventName": "2주차 세션",
                                                "place": "온라인",
                                                "isOnline": true,
                                                "attendanceStatus": "NOT_STARTED"
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
    fun getSessionSelector(): CustomResponse<SessionSelectorResponse>

    @Operation(
        summary = "세션 시간 수정 시 출석 상태 변경 대상 조회",
        description = "세션의 시간이 수정되었을 때 호출하며, 출석 상태가 변경되는 대상을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션 시간 수정 시 출석 상태 변경 대상 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "세션 시간 수정 시 출석 상태 변경 대상 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "GLOBAL-200-1",
                                        "data": {
                                            "targeted": [
                                                {
                                                    "name": "최지우",
                                                    "currentStatus": "ABSENT",
                                                    "targetStatus": "LATE",
                                                    "attendedAt": "2025-08-02T14:10:00"
                                                }
                                            ],
                                            "untargeted": [
                                                {
                                                    "name": "이영희",
                                                    "status": "PRESENT",
                                                    "updatedAt": "2025-08-02T14:05:00"
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
    fun queryTargetAttendancesByPolicyChange(
        sessionId: SessionId,
        attendanceStart: LocalDateTime,
        lateStart: LocalDateTime,
        absentStart: LocalDateTime,
    ): CustomResponse<SessionPolicyUpdateTargetResponse>
}

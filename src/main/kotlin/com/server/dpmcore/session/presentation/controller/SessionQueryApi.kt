package com.server.dpmcore.session.presentation.controller

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.presentation.dto.response.AttendanceTimeResponse
import com.server.dpmcore.session.presentation.dto.response.NextSessionResponse
import com.server.dpmcore.session.presentation.dto.response.SessionDetailResponse
import com.server.dpmcore.session.presentation.dto.response.SessionListResponse
import com.server.dpmcore.session.presentation.dto.response.SessionWeeksResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "세션(Session)")
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
                                        "code": "G000",
                                        "data": {
                                            "sessionId": 1,
                                            "week": 1,
                                            "eventName": "디프만 17기 OT",
                                            "place": "공덕 프론트원",
                                            "isOnline": false,
                                            "date": "2025-08-02T14:00:00.000000"
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
                                        "code": "G000",
                                        "data": {
                                            "sessions": [
                                                {
                                                    "id": 1,
                                                    "week": 1,
                                                    "eventName": "디프만 17기 OT",
                                                    "date": "2025-08-02T14:00:00.000000"
                                                },
                                                {
                                                    "id": 2,
                                                    "week": 2,
                                                    "eventName": "미니 디프콘",
                                                    "date": "2025-08-09T14:00:00.000000"
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
                                name = "세션 상세 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000",
                                        "data": {
                                            "sessionId": 1,
                                            "week": 1,
                                            "eventName": "디프만 17기 OT",
                                            "place": "공덕 프론트원",
                                            "isOnline": false,
                                            "date": "2025-08-02T14:00:00.000000",
                                            "attendanceStartTime": "2025-08-02T14:00:00.000000",
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
    fun getSessionById(sessionId: SessionId): CustomResponse<SessionDetailResponse>

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
                                        "code": "G000",
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
                                        "code": "G000",
                                        "data": {
                                            "weeks": [1, 2, 3, 4]
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
}

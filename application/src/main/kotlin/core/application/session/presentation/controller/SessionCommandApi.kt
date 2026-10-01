package core.application.session.presentation.controller

import core.application.attendance.presentation.request.UpdateAttendanceTimeRequest
import core.application.common.exception.CustomResponse
import core.application.session.presentation.request.SessionCreateRequest
import core.application.session.presentation.request.SessionUpdateRequest
import core.domain.session.vo.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Session Mutation", description = "세션 추가/변경 API")
interface SessionCommandApi {
    @Operation(
        summary = "세션 출석시간 갱신",
        description =
            "세션 ID를 통해 해당 세션의 출석 시작 시간을 갱신합니다. " +
                "출석 시작 < 지각 시작 < 출석 마감 순서를 지켜야 하며(SESSION-400-08), " +
                "세션과 다른 날짜여도 됩니다(예: 00:05 세션의 출석 시작 전날 23:55).",
        requestBody =
            RequestBody(
                description = "세션 출석시간 갱신 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = UpdateAttendanceTimeRequest::class),
                        examples = [
                            ExampleObject(
                                name = "세션 출석시간 갱신 요청 예시",
                                value = """
                                {
                                    "attendanceStartTime": "2025-08-02T14:05:00.000000"
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
                description = "세션 출석시간 갱신 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [],
                    ),
                ],
            ),
        ],
    )
    fun updateAttendanceTime(
        sessionId: SessionId,
        request: UpdateAttendanceTimeRequest,
    ): CustomResponse<Void>

    @Operation(
        summary = "세션 추가",
        description =
            "세션 기본 정보를 입력하고 출결 시간을 설정합니다. " +
                "attendanceStart/lateStart/absentStart 를 모두 생략하면 서버 설정 기본값" +
                "(기본: 시작 10분 전 출석 시작, 15분 후 지각, 30분 후 마감)으로 세션 시작(date) 기준 시각을 계산해 저장합니다. " +
                "기본값을 바꿔도 이미 만든 세션의 시각은 바뀌지 않습니다. " +
                "모두 입력하면 입력값을 이 세션만의 시각으로 사용하며 출석 시작 < 지각 시작 < 출석 마감 순서여야 합니다" +
                "(SESSION-400-08). 일부만 입력하면 SESSION-400-09 입니다.",
        requestBody =
            RequestBody(
                description = "세션 추가 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = SessionCreateRequest::class),
                        examples = [
                            ExampleObject(
                                name = "세션 추가 요청 예시 (출석 시각 명시)",
                                value = """
                                {
                                  "name": "OT & 팀빌딩",
                                  "date": "2025-08-02T14:00:00",
                                  "isOnline": false,
                                  "place": "서울시공익활동지원센터",
                                  "week": 1,
                                  "attendanceStart": "2025-08-02T14:00:00",
                                  "lateStart": "2025-08-02T14:20:00",
                                  "absentStart": "2025-08-02T14:35:00"
                                }
                            """,
                            ),
                            ExampleObject(
                                name = "세션 추가 요청 예시 (서버 기본 출석 시간 적용)",
                                value = """
                                {
                                  "name": "OT & 팀빌딩",
                                  "date": "2025-08-02T14:00:00",
                                  "isOnline": false,
                                  "place": "서울시공익활동지원센터",
                                  "week": 1
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
                description = "세션 추가 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "SESSION-400-08 출석 시각 순서 오류, SESSION-400-09 출석 시각 일부만 입력",
            ),
        ],
    )
    fun createSession(request: SessionCreateRequest): CustomResponse<Void>

    @Operation(
        summary = "세션 수정",
        description =
            "세션을 수정하고 연관된 멤버의 출석 상태를 갱신 합니다. 출석 시각 세 개는 모두 필요하며 " +
                "출석 시작 < 지각 시작 < 출석 마감 순서여야 합니다(SESSION-400-08).",
        requestBody =
            RequestBody(
                description = "세션 수정 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = SessionUpdateRequest::class),
                        examples = [
                            ExampleObject(
                                name = "세션 수정 요청 예시",
                                value = """
                                {
                                  "sessionId": 1,
                                  "name": "OT & 팀빌딩",
                                  "date": "2025-08-02T14:00:00",
                                  "isOnline": false,
                                  "place": "서울시공익활동지원센터",
                                  "week": 1,
                                  "attendanceStart": "2025-08-02T14:00:00",
                                  "lateStart": "2025-08-02T14:20:00",
                                  "absentStart": "2025-08-02T14:35:00"
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
                description = "세션 수정 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun updateSession(request: SessionUpdateRequest): CustomResponse<Void>

    @Operation(
        summary = "세션 삭제",
        description = "세션 ID를 통해 해당 세션을 삭제합니다. 세션이 삭제되면 연관된 출석 정보도 함께 소프트 딜리트 처리 됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "세션 삭제 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun softDeleteSession(sessionId: SessionId): CustomResponse<Void>
}

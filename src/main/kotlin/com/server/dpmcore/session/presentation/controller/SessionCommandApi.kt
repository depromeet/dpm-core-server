package com.server.dpmcore.session.presentation.controller

import com.server.dpmcore.attendance.presentation.dto.request.UpdateAttendanceTimeRequest
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.session.domain.model.SessionId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "세션(Session)")
interface SessionCommandApi {
    @Operation(
        summary = "세션 출석시간 갱신",
        description = "세션 ID를 통해 해당 세션의 출석 시작 시간을 갱신합니다. 출석 시작 시간의 날짜는 세션의 날짜와 동일해야 합니다.",
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
                        examples = [
                            ExampleObject(
                                name = "세션 출석시간 갱신 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공했습니다",
                                        "code": "G000"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateAttendanceTime(
        sessionId: SessionId,
        request: UpdateAttendanceTimeRequest,
    ): CustomResponse<Void>
}

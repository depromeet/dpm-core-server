package com.server.dpmcore.authority.presentation

import com.server.dpmcore.authority.presentation.response.AuthorityListResponse
import com.server.dpmcore.common.exception.CustomResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Authority", description = "권한 API")
interface AuthorityApi {
    @ApiResponse(
        responseCode = "200",
        description = "권한 목록 조회 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "권한 목록 조회 성공 응답",
                        value = """
                            {
                                "status": "OK",
                                "code": "G000",
                                "message": "요청에 성공했습니다",
                                "data": {
                                    "authorities": [
                                        {
                                            "id": 1,
                                            "name": "ORGANIZER"
                                        },
                                        {
                                            "id": 2,
                                            "name": "17_DEEPER"
                                        }
                                    ]
                                }
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @Operation(
        summary = "권한 목록 조회 API",
        description =
            "모든 권한 목록을 조회 합니다. " +
                "정산서 생성 시 권한 별로 회식 인원을 초대하기 위해 사용 됩니다.",
    )
    fun getAllAuthorities(): CustomResponse<AuthorityListResponse>
}

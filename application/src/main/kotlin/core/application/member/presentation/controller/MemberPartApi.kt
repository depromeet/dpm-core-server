package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.response.MemberPartsResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member Part", description = "멤버 파트 API")
interface MemberPartApi {
    @ApiResponse(
        responseCode = "200",
        description = "파트 선택지 조회 성공",
        content = [
            Content(
                mediaType = "application/json",
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "파트 선택지 조회 성공 응답",
                        value = """
                            {
                                "status": "OK",
                                "message": "요청에 성공했습니다",
                                "code": "GLOBAL-200-01",
                                "data": {
                                    "parts": ["WEB", "SERVER", "DESIGN", "IOS", "ANDROID", "UNASSIGNED"]
                                }
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @Operation(
        summary = "파트 선택지 조회 API (운영진)",
        description =
            "출석 명단의 파트 필터 선택지를 조회합니다. 파라미터는 없습니다. " +
                "파트 정의 순서(WEB, SERVER, DESIGN, IOS, ANDROID) 뒤에 UNASSIGNED 를 붙이며 전체(ALL) 선택지는 없습니다. " +
                "UNASSIGNED 는 저장되는 파트 값이 아니라 명단의 part 가 null 인 멤버를 고르는 선택지입니다.",
    )
    fun getParts(): CustomResponse<MemberPartsResponse>
}

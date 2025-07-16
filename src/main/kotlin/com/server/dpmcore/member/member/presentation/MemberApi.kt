package com.server.dpmcore.member.member.presentation

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.presentation.response.MemberDetailsResponse
import com.server.dpmcore.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Member", description = "멤버 API")
interface MemberApi {
    @ApiResponse(
        responseCode = "200",
        description = "로그인 한 멤버 조회 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "로그인 한 멤버 조회 성공 응답",
                        value = """
                            {
                                "status": "OK",
                                "code": "G000",
                                "message": "요청에 성공했습니다",
                                "data": {
                                    "email": "depromeetcore@gmail.com",
                                    "name": "디프만"
                                }
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @Operation(summary = "로그인 한 멤버 조회 API", description = "현재 로그인한 멤버의 기본 정보를 조회 합니다.")
    fun me(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<MemberDetailsResponse>

    @ApiResponse(
        responseCode = "204",
        description = "회원 탈퇴 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "회원 탈퇴 성공 응답",
                        value = """
                            {
                                "status": "OK",
                                "code": "G000",
                                "message": "요청에 성공했습니다",
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @Operation(
        summary = "회원 탈퇴 API",
        description = "현재 로그인한 멤버를 탈퇴 처리합니다. 탈퇴 후에는 해당 멤버의 모든 정보가 삭제됩니다.",
    )
    fun withdraw(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void>
}

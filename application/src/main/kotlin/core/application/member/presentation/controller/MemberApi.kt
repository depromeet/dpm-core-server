package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.InitMemberDataRequest
import core.application.member.presentation.request.UpdateMemberStatusRequest
import core.application.member.presentation.request.WhiteListCheckRequest
import core.application.member.presentation.response.MemberDetailsResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
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
                                    "name": "디프만",
                                    "part": "WEB",
                                    "cohort": "17",
                                    "teamNumber": 3,
                                    "isAdmin": false,
                                    "status": "ACTIVE"
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
        responseCode = "200",
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
                                "message": "요청에 성공했습니다"
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @Operation(
        summary = "멤버 탈퇴 API",
        description = "현재 로그인한 멤버를 탈퇴 처리합니다. 탈퇴 후에는 해당 멤버의 모든 정보가 삭제됩니다.",
    )
    fun withdraw(
        @CurrentMemberId memberId: MemberId,
        response: HttpServletResponse,
    ): CustomResponse<Void>

    @Operation(
        summary = "멤버 데이터 주입 및 승인 API (dev)",
        description = "멤버의 추가적인 데이터를 주입하고, 승인 상태로 변경합니다. 관리자가 멤버 가입 시 데이터를 변경할 때 사용됩니다.",
        requestBody =
            RequestBody(
                content = [
                    Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = InitMemberDataRequest::class),
                        examples = [
                            ExampleObject(
                                name = "멤버 데이터 주입 요청 예시",
                                value = """
                                {
                                    "teamId": "1",
                                    "members": [
                                        {
                                            "memberId": "1",
                                            "memberPart": "SERVER"
                                        },
                                        {
                                            "memberId": "2",
                                            "memberPart": "DESIGN"
                                        }
                                    ]
                                }
                            """,
                            ),
                        ],
                    ),
                ],
            ),
    )
    @ApiResponse(
        responseCode = "200",
        description = "멤버 데이터 주입 및 승인 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
            ),
        ],
    )
    fun initMemberDataAndApprove(request: InitMemberDataRequest): CustomResponse<Void>

    @Operation(
        summary = "가입된 이메일인지 체크하는 화이트리스트 API",
        description = "주어진 이메일이 화이트리스트에 포함되어 있으면 가입 승인 합니다.",
    )
    @ApiResponse(responseCode = "200", description = "화이트리스트 체크 및 승인 성공")
    fun checkWhiteList(request: WhiteListCheckRequest): CustomResponse<Void>

    @Operation(
        summary = "멤버 상태 변경 API (dev)",
        description = "멤버의 상태를 변경합니다. 개발 중 멤버 상태를 컨트롤하기 위해 사용합니다.(PENDING/ACTIVE)",
        requestBody =
            RequestBody(
                content = [
                    Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = InitMemberDataRequest::class),
                        examples = [
                            ExampleObject(
                                name = "멤버 상태 변경 요청 예시",
                                value = """
                                {
                                    "memberId": "1",
                                    "memberStatus": "PENDING"
                                }
                            """,
                            ),
                        ],
                    ),
                ],
            ),
    )
    @ApiResponse(
        responseCode = "200",
        description = "멤버 상태 변경 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
            ),
        ],
    )
    fun updateMemberStatus(request: UpdateMemberStatusRequest): CustomResponse<Void>
}

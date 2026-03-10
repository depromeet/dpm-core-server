package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.ConvertDeeperToOrganizerRequest
import core.application.member.presentation.request.InitMemberDataRequest
import core.application.member.presentation.request.UpdateMemberStatusRequest
import core.application.member.presentation.request.WhiteListCheckRequest
import core.application.member.presentation.response.MemberDetailsResponse
import core.application.member.presentation.response.MemberOverviewResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.cohort.vo.CohortId
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
        description = "멤버 목록 조회 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "멤버 목록 조회 응답 예시",
                        value = """
                            {
                                "status": "OK",
                                "code": "G000",
                                "message": "요청에 성공했습니다",
                                "data": {
                                    "members": [
                                        {
                                            "memberId": 1,
                                            "cohortId": 17,
                                            "name": "홍길동",
                                            "teamName": "1팀",
                                            "status": "PENDING",
                                            "part": "UNASSIGNED"
                                        },
                                        {
                                            "memberId": 2,
                                            "cohortId": 17,
                                            "name": "김철수",
                                            "teamName": "2팀",
                                            "status": "ACTIVE",
                                            "part": "SERVER"
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
        summary = "멤버 목록 조회 API",
        description = "최신 기수 우선, 상태(PENDING > INACTIVE > ACTIVE > 기타) 우선순위로 멤버 목록을 조회합니다.",
    )
    fun getMembersOverview(): CustomResponse<MemberOverviewResponse>

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
                                    "members": [
                                        {
                                            "memberId": "1",
                                            "memberPart": "SERVER",
                                            "team": "1",
                                            "status": "ACTIVE"
                                        },
                                        {
                                            "memberId": "2",
                                            "memberPart": "DESIGN",
                                            "team": "2",
                                            "status": "INACTIVE"
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
        summary = "멤버 승인용 화이트리스트 API",
        description = "주어진 멤버 ID 숫자 목록을 승인합니다.",
        requestBody =
            RequestBody(
                content = [
                    Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = WhiteListCheckRequest::class),
                        examples = [
                            ExampleObject(
                                name = "화이트리스트 체크 요청 예시",
                                value = """
                                {
                                    "members": [
                                        1,
                                        2,
                                        3
                                    ]
                                }
                            """,
                            ),
                        ],
                    ),
                ],
            ),
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

    @Operation(
        summary = "DEEPER를 ORGANIZER로 변환 API (dev)",
        description = "지정한 멤버의 권한을 DEEPER에서 ORGANIZER로 변환합니다.",
    )
    @ApiResponse(responseCode = "200", description = "권한 변환 성공")
    fun convertDeeperToOrganizer(request: ConvertDeeperToOrganizerRequest): CustomResponse<Void>

    @Operation(
        summary = "신규 기수 참여 회원 init API (dev)",
        description =
            "신규 기수 참여 회원에 대해 초기화 합니다.\n" +
                "해당 기수 출석부, 공지/과제, 회식 참여 등에 해당 멤버를 추가합니다.",
    )
    @ApiResponse(responseCode = "200", description = "신규 기수 참여 회원 init 성공")
    fun initMemberCohort(
        memberId: MemberId,
        cohortId: CohortId,
    ): CustomResponse<Void>
}

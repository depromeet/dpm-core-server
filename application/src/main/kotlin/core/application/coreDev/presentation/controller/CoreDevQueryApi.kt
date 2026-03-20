package core.application.coreDev.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.coreDev.presentation.response.CoreDevMemberDetailResponse
import core.application.coreDev.presentation.response.CoreDevMemberListResponse
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "CoreDev", description = "코어 개발용 API")
interface CoreDevQueryApi {
    @ApiResponse(
        responseCode = "200",
        description = "가입한 모든 멤버 조회 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "가입한 모든 멤버 조회 성공 응답",
                        value = """
                            {
                                "status": "OK",
                                "code": "GLOBAL-200-01",
                                "message": "요청에 성공했습니다",
                                "data": {
                                    "members": [
                                      {
                                        "email": "smc9919@naver.com",
                                        "name": "신민철",
                                        "part": "SERVER",
                                        "cohortInfos": [
                                          {
                                            "cohort": "17",
                                            "teamNumber": 1,
                                            "isAdmin": true
                                          }
                                        ],
                                        "status": "ACTIVE"
                                      },
                                      {
                                        "email": "wjdwnsdnjs13@naver.com",
                                        "name": "정준원",
                                        "part": "SERVER",
                                        "cohortInfos": [
                                          {
                                            "cohort": "18",
                                            "teamNumber": 1,
                                            "isAdmin": true
                                          }
                                        ],
                                        "status": "ACTIVE"
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
    @Operation(summary = "가입한 모든 멤버 조회 API", description = "현재 가입한 모든 멤버의 정보를 조회 합니다.")
    fun allMember(memberId: MemberId): CustomResponse<CoreDevMemberListResponse>

    @ApiResponse(
        responseCode = "200",
        description = "이메일 멤버 정보 조회 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "이메일 멤버 정보 조회 성공 응답",
                        value = """
                            {
                              "status": "OK",
                              "message": "요청에 성공했습니다",
                              "code": "GLOBAL-200-01",
                              "data": {
                                "email": "wjdwnsdnjs13@naver.com",
                                "name": "정준원",
                                "part": "SERVER",
                                "cohortInfos": [
                                  {
                                    "cohort": "18",
                                    "teamNumber": 1,
                                    "isAdmin": true
                                  }
                                ],
                                "status": "ACTIVE"
                              }
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @Operation(summary = "이메일 멤버 정보 조회 API", description = "이메일의 멤버 정보를 조회 합니다.")
    fun memberInfo(
        memberId: MemberId,
        memberEmail: String,
    ): CustomResponse<CoreDevMemberDetailResponse>
}

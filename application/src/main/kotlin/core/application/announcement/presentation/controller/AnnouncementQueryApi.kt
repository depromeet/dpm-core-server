package core.application.announcement.presentation.controller

import core.application.announcement.presentation.response.AnnouncementDetailResponse
import core.application.announcement.presentation.response.AnnouncementListResponse
import core.application.common.exception.CustomResponse
import core.domain.announcement.vo.AnnouncementId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Announcement", description = "공지/과제 API")
interface AnnouncementQueryApi {
    @ApiResponse(
        responseCode = "200",
        description = "공지/과제 목록",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "공지/과제 목록 조회 성공 응답",
                        value = """
                            {
                              "status": "OK",
                              "message": "요청에 성공했습니다",
                              "code": "GLOBAL-200-01",
                              "data": {
                                "announcementCount": 1,
                                "announcements": [
                                  {
                                    "announcementId": 3,
                                    "title": "공지/과제 테스트용 제목 001",
                                    "announcementType": "ASSIGNMENT",
                                    "assignmentType": "INDIVIDUAL",
                                    "createdAt": "2026-01-30T07:15:36.977721",
                                    "readMemberCount": 0
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
        summary = "공지/과제 목록 조회 API",
        description = "공지/과제 목록을 조회합니다",
    )
    fun getAnnouncementList(): CustomResponse<AnnouncementListResponse>

    @ApiResponse(
        responseCode = "200",
        description = "공지/과제 상세",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "공지/과제 상세 조회 성공 응답",
                        value = """
                            {
                              "status": "OK",
                              "message": "요청에 성공했습니다",
                              "code": "GLOBAL-200-01",
                              "data": {
                                "announcementCount": 1,
                                "announcements": [
                                  {
                                    "announcementId": 3,
                                    "title": "공지/과제 테스트용 제목 001",
                                    "announcementType": "ASSIGNMENT",
                                    "assignmentType": "INDIVIDUAL",
                                    "createdAt": "2026-01-30T07:15:36.977721",
                                    "readMemberCount": 0
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
        summary = "공지/과제 상세 조회 API",
        description = "공지/과제 상세 내용을 조회합니다",
    )
    fun getAnnouncementDetail(announcementId: AnnouncementId): CustomResponse<AnnouncementDetailResponse>
}

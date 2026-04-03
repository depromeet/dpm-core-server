package core.application.notification.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.notification.presentation.request.DeletePushTokenRequest
import core.application.notification.presentation.request.MessageTypeNotificationRequest
import core.application.notification.presentation.request.NotificationRequest
import core.application.notification.presentation.request.RegisterPushTokenRequest
import core.application.notification.presentation.response.NotificationTypeResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Notification", description = "FCM 알림 API")
interface NotificationApi {
    @ApiResponse(
        responseCode = "200",
        description = "푸시 토큰 등록 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "푸시 토큰 등록 성공 응답",
                        value = """
                            {
                                "status": "OK",
                                "code": "G000",
                                "message": "요청에 성공했습니다",
                                "data": {
                                    "id": 1,
                                    "memberId": 13,
                                    "token": "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
                                    "createdAt": "2026-03-27T00:00:00Z",
                                    "updatedAt": "2026-03-27T00:00:00Z"
                                }
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @Operation(summary = "푸시 토큰 등록 API", description = "멤버의 푸시 토큰을 등록하거나 갱신합니다")
    fun registerPushToken(
        memberId: MemberId,
        request: RegisterPushTokenRequest,
    ): CustomResponse<Void>

    @ApiResponse(
        responseCode = "200",
        description = "푸시 토큰 삭제 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "푸시 토큰 삭제 성공 응답",
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
    @Operation(summary = "푸시 토큰 삭제 API", description = "멤버의 푸시 토큰을 삭제합니다 (로그아웃 시)")
    fun deletePushToken(
        @CurrentMemberId memberId: MemberId,
        request: DeletePushTokenRequest,
    ): CustomResponse<Void>

    @ApiResponse(
        responseCode = "200",
        description = "모든 푸시 토큰 삭제 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "모든 푸시 토큰 삭제 성공 응답",
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
    @Operation(summary = "모든 푸시 토큰 삭제 API", description = "멤버의 모든 푸시 토큰을 삭제합니다 (회원 탈퇴 시)")
    fun deleteAllPushTokens(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void>

    @ApiResponse(
        responseCode = "200",
        description = "Expo 커스텀 알림 테스트 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "Expo 커스텀 알림 성공 응답",
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
    @Operation(summary = "Expo 커스텀 알림 테스트 API", description = "Expo 커스텀 알림을 테스트합니다.")
    fun testSendNotification(notificationRequest: NotificationRequest): CustomResponse<Void>

    @ApiResponse(
        responseCode = "200",
        description = "Expo 알림 테스트 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "Expo 알림 성공 응답",
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
    @Operation(summary = "Expo 알림 테스트 API", description = "Expo 알림을 테스트합니다.")
    fun testSendMessageTypeNotification(
        messageTypeNotificationRequest: MessageTypeNotificationRequest,
    ): CustomResponse<Void>

    @ApiResponse(
        responseCode = "200",
        description = "알림 타입 목록 조회 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "알림 타입 목록 조회 성공 응답",
                        value = """
                            {
                                "status": "OK",
                                "code": "G000",
                                "message": "요청에 성공했습니다",
                                "data": [
                                    {
                                        "name": "SESSION_START_SOON",
                                        "title": "세션 시작 30분 전 알림",
                                        "bodyTemplate": "{sessionName} 세션이 30분 후 시작됩니다!",
                                        "description": "세션 시작 30분 전 알림"
                                    }
                                ]
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @Operation(summary = "알림 타입 목록 조회 API", description = "모든 알림 타입(NotificationMessage)을 조회합니다.")
    fun getNotificationTypes(): CustomResponse<List<NotificationTypeResponse>>
}

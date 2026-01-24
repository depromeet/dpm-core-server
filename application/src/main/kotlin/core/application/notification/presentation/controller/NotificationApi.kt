package core.application.notification.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.notification.presentation.request.NotificationRequest
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
        description = "FCM알림 테스트 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "FCM 알림 성공 응답",
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
    @Operation(summary = "FCM 알림 테스트 API", description = "FCM 알림을 테스트합니다.")
    fun testSendNotification(notificationRequest: NotificationRequest): CustomResponse<Void>
}

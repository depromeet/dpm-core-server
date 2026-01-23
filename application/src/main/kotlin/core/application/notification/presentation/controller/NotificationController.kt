package core.application.notification.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.notification.application.service.NotificationService
import core.application.notification.presentation.request.NotificationRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    val notificationService: NotificationService,
) : NotificationApi {
    @PostMapping
    override fun testSendNotification(notificationRequest: NotificationRequest): CustomResponse<Void> {
        val messageId: Boolean =
            notificationService.sendPushNotification(
                notificationRequest.message,
                notificationRequest.title,
                notificationRequest.message,
            )
        return CustomResponse.ok()
    }
}

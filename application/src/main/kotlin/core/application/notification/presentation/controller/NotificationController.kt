package core.application.notification.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.notification.application.service.NotificationCommandService
import core.application.notification.presentation.request.DeletePushTokenRequest
import core.application.notification.presentation.request.MessageTypeNotificationRequest
import core.application.notification.presentation.request.NotificationRequest
import core.application.notification.presentation.request.RegisterPushTokenRequest
import core.application.notification.presentation.response.NotificationTypeResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.member.vo.MemberId
import core.domain.notification.enums.NotificationMessageType
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/notifications")
class NotificationController(
    val notificationService: NotificationCommandService,
) : NotificationApi {
    @PreAuthorize("hasAuthority('read:member')")
    @PostMapping("/tokens")
    override fun registerPushToken(
        @CurrentMemberId memberId: MemberId,
        @Valid @RequestBody request: RegisterPushTokenRequest,
    ): CustomResponse<Void> {
        notificationService.registerPushToken(
            memberId = memberId,
            token = request.token,
        )

        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('read:member')")
    @DeleteMapping("/tokens")
    override fun deletePushToken(
        @CurrentMemberId memberId: MemberId,
        @Valid @RequestBody request: DeletePushTokenRequest,
    ): CustomResponse<Void> {
        notificationService.deletePushToken(
            memberId = memberId,
            token = request.token,
        )

        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('read:member')")
    @DeleteMapping("/tokens/all")
    override fun deleteAllPushTokens(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        notificationService.deleteAllPushTokens(memberId)
        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('read:member')")
    @PostMapping("/custom-test")
    override fun testSendNotification(
        @RequestBody notificationRequest: NotificationRequest,
    ): CustomResponse<Void> {
        notificationService.sendCustomPushNotification(
            memberId = notificationRequest.targetMemberId,
            title = notificationRequest.title,
            body = notificationRequest.message,
            data = null,
        )
        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('read:member')")
    @PostMapping("/type-test")
    override fun testSendMessageTypeNotification(
        @RequestBody messageTypeNotificationRequest: MessageTypeNotificationRequest,
    ): CustomResponse<Void> {
        notificationService.sendPushNotification(
            memberId = messageTypeNotificationRequest.targetMemberId,
            messageType = messageTypeNotificationRequest.notificationMessageType,
            variables = emptyMap(),
            data = null,
        )
        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('read:member')")
    @GetMapping("/types")
    override fun getNotificationTypes(): CustomResponse<List<NotificationTypeResponse>> {
        val types = NotificationMessageType.entries.map { NotificationTypeResponse.from(it) }
        return CustomResponse.ok(types)
    }
}

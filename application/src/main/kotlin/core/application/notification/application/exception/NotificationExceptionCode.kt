package core.application.notification.application.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class NotificationExceptionCode(
    @JvmField
    val status: HttpStatus,
    @JvmField
    val code: String,
    @JvmField
    val message: String,
) : ExceptionCode {
    NOTIFICATION_FAILED(HttpStatus.BAD_REQUEST, "NOTIFICATION-400-01", "알림 발송에 실패했습니다"),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}

package core.application.notification.application.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class NotificationExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    NOTIFICATION_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION-404-01", "푸시 토큰을 찾을 수 없습니다"),
    INVALID_NOTIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "NOTIFICATION-400-01", "유효하지 않은 푸시 토큰입니다"),
    NOTIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NOTIFICATION-500-01", "알림 발송에 실패했습니다"),
    ;

    override fun getStatus(): HttpStatus = this.status

    override fun getCode(): String = this.code

    override fun getMessage(): String = this.message
}

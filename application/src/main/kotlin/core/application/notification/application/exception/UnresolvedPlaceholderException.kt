package core.application.notification.application.exception

import core.application.common.exception.BusinessException

class UnresolvedPlaceholderException(
    unresolvedPlaceholders: List<String>,
    messageTypeName: String,
) : BusinessException(
        NotificationExceptionCode.UNRESOLVED_PLACEHOLDER,
    ) {
    override val message: String =
        "미치환 플레이스홀더가 남아있습니다: $unresolvedPlaceholders (messageType=$messageTypeName)"
}

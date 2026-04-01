package core.domain.notification.enums

enum class NotificationMessage(
    val title: String,
    val bodyTemplate: String,
    val description: String,
) {
    SESSION_START_SOON(
        title = "세션 시작 30분 전 알림",
        bodyTemplate = "{sessionName} 세션이 30분 후 시작됩니다!",
        description = "세션 시작 30분 전 알림",
    ),
    SESSION_STARTED(
        title = "세션 시작",
        bodyTemplate = "{sessionName} 세션이 시작되었습니다. 출석해주세요!",
        description = "세션 시작 시 알림",
    ),
    ANNOUNCEMENT_NEW(
        title = "새 공지사항",
        bodyTemplate = "{title}",
        description = "새 공지사항 등록 시 알림",
    ),
    AFTER_PARTY_INVITATION(
        title = "회식 생성",
        bodyTemplate = "{eventName} 회식에 초대되었습니다.",
        description = "회식 생성 시 디퍼 알림 발송",
    ),
    ;

    fun format(variables: Map<String, Any>): String {
        var result = bodyTemplate
        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        return result
    }

    fun formatWithTitle(variables: Map<String, Any>): Pair<String, String> = title to format(variables)
}

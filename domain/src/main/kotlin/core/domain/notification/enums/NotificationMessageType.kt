package core.domain.notification.enums

enum class NotificationMessageType(
    val title: String,
    val bodyTemplate: String,
    val description: String,
) {
    SESSION_START_SOON(
        title = "세션 시작 30분 전 알림",
        bodyTemplate = "{title} 세션이 30분 후 시작됩니다!",
        description = "세션 시작 30분 전 알림",
    ),
    SESSION_STARTED(
        title = "세션 시작",
        bodyTemplate = "{title} 세션이 시작되었습니다. 출석해주세요!",
        description = "세션 시작 시 알림",
    ),
    ANNOUNCEMENT_NEW(
        title = "새로운 공지가 등록됐어요.",
        bodyTemplate = "{title}",
        description = "새 공지사항 등록 시 알림",
    ),
    AFTER_PARTY_INVITATION(
        title = "새로운 회식이 열렸어요.",
        bodyTemplate = "{title}",
        description = "회식 생성 시 디퍼 알림 발송",
    ),
    AFTER_PARTY_REMIND(
        title = "회식에 참여 여부를 남기지 않았어요.",
        bodyTemplate = "{title}",
        description = "회식 미제출자 리마인드 알림",
    ),
    ANNOUNCEMENT_REMIND(
        title = "아직 읽지 않은 공지가 있어요.",
        bodyTemplate = "{title}",
        description = "공지 미열람자 리마인드 알림",
    ),
    ASSIGNMENT_NEW(
        title = "새로운 과제가 등록됐어요.",
        bodyTemplate = "{title}",
        description = "과제 등록 시 알림",
    ),
    ASSIGNMENT_SUBMIT_REQUEST(
        title = "아직 제출하지 않은 과제가 있어요.",
        bodyTemplate = "{title}",
        description = "과제 미제출자 제출요청 알림",
    ),
    ASSIGNMENT_SUBMIT_REQUEST_TO_MEMBERS(
        title = "과제를 제출해주세요!",
        bodyTemplate = "{title}",
        description = "선택한 멤버 과제 제출요청 알림",
    ),
    ASSIGNMENT_DUE_24H(
        title = "과제 제출까지 하루 남았어요.",
        bodyTemplate = "잊으신 건 아니죠? 내일 이 시간은 과제 제출 마감이에요. 미리 확인해 보세요!",
        description = "과제 제출 24시간 전 알림",
    ),
    ASSIGNMENT_DUE_12H(
        title = "과제 제출까지 하루도 남지 않았어요.",
        bodyTemplate = "아직 과제를 작성 중이신가요? 12시간 뒤면 제출 창이 닫히니 조금만 더 힘내세요!",
        description = "과제 제출 12시간 전 알림",
    ),
    ASSIGNMENT_DUE_1H(
        title = "제출까지 한시간도 남지 않았어요",
        bodyTemplate = "지금 바로 제출하지 않으면 링크가 막혀요! 완성한 과제를 확인해주세요!",
        description = "과제 제출 1시간 전 알림",
    ),
    ATTENDANCE_STARTED(
        title = "출석체크가 시작됐어요.",
        bodyTemplate = "코드를 확인하고 출석을 완료해주세요.",
        description = "출석체크 시작 시 알림",
    ),
    SESSION_DAY_BEFORE(
        title = "내일 진행되는 세션 정보를 확인해주세요.",
        bodyTemplate = "{title}",
        description = "세션 24시간 전 알림",
    ),
    ;

    fun format(variables: Map<String, Any>): String {
        var result = bodyTemplate
        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }

        val unresolvedPlaceholders = Regex("\\{\\w+}").findAll(result).map { it.value }.toList()
        require(unresolvedPlaceholders.isEmpty()) {
            "미치환 플레이스홀더가 남아있습니다: $unresolvedPlaceholders (messageType=$name)"
        }

        return result
    }

    fun formatWithTitle(variables: Map<String, Any>): Pair<String, String> {
        var formattedTitle = title
        variables.forEach { (key, value) ->
            formattedTitle = formattedTitle.replace("{$key}", value.toString())
        }
        return formattedTitle to format(variables)
    }
}

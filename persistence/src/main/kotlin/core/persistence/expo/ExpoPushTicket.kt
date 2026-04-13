package core.persistence.expo

/**
 * Expo Push 알림 발송 티켓
 *
 * @property status 발송 상태 ("ok" 또는 "error")
 * @property id Receipt ID (status가 "ok"일 때)
 * @property message 에러 메시지 (status가 "error"일 때)
 * @property details 에러 상세 정보
 */
data class ExpoPushTicket(
    val status: String,
    val id: String? = null,
    val message: String? = null,
    val details: Map<String, Any>? = null,
)

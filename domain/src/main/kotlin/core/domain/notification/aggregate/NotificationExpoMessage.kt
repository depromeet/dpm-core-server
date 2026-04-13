package core.domain.notification.aggregate

/**
 * Expo Push Notification API에 전송하는 메시지 데이터 모델
 *
 * Expo Push Notification Service (https://exp.host/--/api/v2/push/send)로 전송되는
 * 푸시 알림 메시지의 구조를 정의합니다.
 *
 * @see <a href="https://docs.expo.dev/push-notifications/sending-notifications/">Expo Push Notifications</a>
 */
data class NotificationExpoMessage(
    /**
     * Expo Push Token
     *
     * 알림을 받을 디바이스의 Expo Push Token입니다.
     * 형식: "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]"
     *
     * 단일 토큰 또는 토큰 배열(최대 100개)을 지정할 수 있습니다.
     */
    val to: String,
    /**
     * 알림 제목
     *
     * Android와 iOS 모두에서 알림 상단에 표시되는 제목입니다.
     * 최대한 간결하고 명확하게 작성하는 것을 권장합니다.
     */
    val title: String,
    /**
     * 알림 본문
     *
     * 알림의 주요 메시지 내용입니다.
     * Android와 iOS 모두에서 제목 아래에 표시됩니다.
     */
    val body: String,
    /**
     * 커스텀 데이터
     *
     * 앱에서 처리할 추가 데이터를 전달합니다.
     * 알림 탭 시 특정 화면으로 이동하거나 특정 동작을 수행할 때 사용합니다.
     *
     * 예시:
     * ```
     * {
     *   "screen": "DetailScreen",
     *   "id": "123",
     *   "type": "message"
     * }
     * ```
     *
     * 주의: 전체 페이로드는 4KB를 초과할 수 없습니다.
     */
    val data: Map<String, Any>? = null,
    /**
     * 우선순위
     *
     * 알림 전송 우선순위를 지정합니다.
     * - "default": 기본 우선순위
     * - "normal": 일반 우선순위 (배터리 절약 모드에서 지연될 수 있음)
     * - "high": 높은 우선순위 (즉시 전송)
     *
     * 중요한 알림은 "high"를 사용하고, 일반적인 알림은 "default"를 권장합니다.
     */
    val priority: String,
    /**
     * 알림음
     *
     * 알림 수신 시 재생할 사운드를 지정합니다.
     * - "default": 기본 알림음 사용
     * - null: 무음
     *
     * 커스텀 사운드를 사용하려면 앱에 사운드 파일을 포함시켜야 합니다.
     */
    val sound: String,
    /**
     * 배지 숫자 (iOS 전용)
     *
     * iOS 앱 아이콘에 표시될 배지 숫자입니다.
     * null이면 배지가 변경되지 않습니다.
     *
     * Android에서는 무시됩니다.
     */
    val badge: Int? = null,
    /**
     * 알림 채널 ID (Android 전용)
     *
     * Android 8.0(API 26) 이상에서 사용되는 알림 채널 ID입니다.
     * 앱에서 미리 생성한 채널 ID와 일치해야 합니다.
     *
     * 지정하지 않으면 기본 채널이 사용됩니다.
     * iOS에서는 무시됩니다.
     */
    val channelId: String? = null,
    /**
     * Time To Live (유효 시간)
     *
     * 알림이 전송되지 못했을 때 Expo 서버에서 재시도할 시간(초 단위)입니다.
     * 0 ~ 2,419,200초 (28일) 범위에서 지정할 수 있습니다.
     *
     * - 0: 즉시 전송 실패 시 재시도하지 않음
     * - 기본값: 지정하지 않으면 28일
     *
     * 시간에 민감한 알림(예: OTP, 실시간 알림)은 짧게 설정하고,
     * 중요한 정보성 알림은 길게 설정하는 것을 권장합니다.
     */
    val ttl: Int? = null,
    /**
     * 만료 시간
     *
     * 알림이 만료되는 시간을 Unix timestamp (초 단위)로 지정합니다.
     * 이 시간이 지나면 알림이 전송되지 않습니다.
     *
     * ttl과 expiration 중 하나만 사용하는 것을 권장합니다.
     * 둘 다 지정되면 expiration이 우선합니다.
     */
    val expiration: Long? = null,
    /**
     * 부제목 (iOS 전용)
     *
     * iOS 알림에서 제목과 본문 사이에 표시되는 부제목입니다.
     * 알림을 더 세분화하여 설명할 때 유용합니다.
     *
     * Android에서는 무시됩니다.
     *
     * 예시: title="새 메시지", subtitle="john@example.com", body="안녕하세요!"
     */
    val subtitle: String? = null,
    /**
     * 알림 카테고리 ID (iOS 전용)
     *
     * iOS에서 정의한 알림 카테고리 ID입니다.
     * 이를 통해 알림에 액션 버튼(예: 답장, 삭제 등)을 추가할 수 있습니다.
     *
     * 앱에서 미리 UNNotificationCategory로 등록한 카테고리 ID와 일치해야 합니다.
     * Android에서는 무시됩니다.
     */
    val categoryId: String? = null,
    /**
     * Mutable Content (iOS 전용)
     *
     * iOS Notification Service Extension을 사용할지 여부를 지정합니다.
     * true로 설정하면 알림이 표시되기 전에 알림 내용을 수정할 수 있습니다.
     *
     * 사용 예시:
     * - 이미지 다운로드 및 첨부
     * - 알림 내용 암호화/복호화
     * - 알림 내용 동적 변경
     *
     * Android에서는 무시됩니다.
     */
    val mutableContent: Boolean? = null,
)

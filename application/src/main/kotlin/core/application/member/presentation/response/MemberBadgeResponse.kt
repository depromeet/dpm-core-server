package core.application.member.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "현재 기수의 운영진 공통 NEW 상태. 목록의 검색·필터와 무관")
data class MemberBadgesResponse(
    @field:Schema(description = "현재 관리 기수 ID", example = "19")
    val cohortId: Long,
    @field:Schema(description = "PENDING, INCOMPLETE, AT_RISK 세 카드의 상태")
    val cards: List<MemberBadgeResponse>,
)

data class MemberBadgeResponse(
    val card: MemberBadgeCard,
    @field:Schema(description = "운영진 공통 미확인 여부")
    val hasNew: Boolean,
    @field:Schema(description = "기수·카드 내 신규 진입마다 증가하는 버전. 최초 0. 확인 요청에 그대로 전달", example = "3")
    val version: Long,
)

enum class MemberBadgeCard {
    PENDING,
    INCOMPLETE,
    AT_RISK,
}

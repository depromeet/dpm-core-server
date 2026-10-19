package core.application.member.presentation.response

/** 파트 필터 선택지. MemberPart 정의 순서 뒤에 파트 미지정(UNASSIGNED)을 붙인다 */
data class MemberPartsResponse(
    val parts: List<String>,
)

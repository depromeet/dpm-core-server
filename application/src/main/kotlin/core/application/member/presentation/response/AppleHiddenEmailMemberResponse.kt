package core.application.member.presentation.response

data class AppleHiddenEmailMemberResponse(
    val memberId: Long,
    val name: String,
    val part: String?,
    val email: String,
)

data class AppleHiddenEmailMembersResponse(
    val members: List<AppleHiddenEmailMemberResponse>,
)

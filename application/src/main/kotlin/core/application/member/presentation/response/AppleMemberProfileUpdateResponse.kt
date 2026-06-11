package core.application.member.presentation.response

data class AppleMemberProfileUpdateResponse(
    val memberId: Long,
    val name: String,
    val part: String?,
)

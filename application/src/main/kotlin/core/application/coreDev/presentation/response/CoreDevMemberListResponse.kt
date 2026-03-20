package core.application.coreDev.presentation.response

data class CoreDevMemberListResponse(
    val members: List<CoreDevMemberDetailResponse>,
) {
    companion object {
        fun from(members: List<CoreDevMemberDetailResponse>): CoreDevMemberListResponse =
            CoreDevMemberListResponse(
                members = members,
            )
    }
}

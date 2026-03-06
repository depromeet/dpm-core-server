package core.application.announcement.presentation.response

data class AssignmentStatusMemberListResponse(
    val members: List<AssignmentStatusMemberListItemResponse>,
) {
    companion object {
        fun from(members: List<AssignmentStatusMemberListItemResponse>): AssignmentStatusMemberListResponse =
            AssignmentStatusMemberListResponse(
                members = members,
            )
    }
}

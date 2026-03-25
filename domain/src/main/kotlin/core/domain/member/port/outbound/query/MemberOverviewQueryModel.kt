package core.domain.member.port.outbound.query

data class MemberOverviewQueryModel(
    val memberId: Long,
    val cohortId: Long?,
    val cohortValue: String?,
    val name: String,
    val teamNumber: Int,
    val isAdmin: Boolean,
    val status: String,
    val part: String?,
)

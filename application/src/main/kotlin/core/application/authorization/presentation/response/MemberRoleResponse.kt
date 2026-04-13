package core.application.authorization.presentation.response

import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.media.Schema

data class MemberRoleResponse(
    @field:Schema(
        description = "멤버 식별자",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val memberId: Long,
    @field:Schema(
        description = "멤버에게 부여된 역할 이름 목록",
        example = "[\"17기 운영진\"]",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val roles: List<String>,
) {
    companion object {
        fun of(
            memberId: MemberId,
            roles: List<String>,
        ): MemberRoleResponse =
            MemberRoleResponse(
                memberId = memberId.value,
                roles = roles,
            )
    }
}

data class MemberRoleListResponse(
    val members: List<MemberRoleResponse>,
) {
    companion object {
        fun from(memberRoles: Map<MemberId, List<String>>): MemberRoleListResponse =
            MemberRoleListResponse(
                members =
                    memberRoles.map { (memberId, roles) ->
                        MemberRoleResponse.of(memberId, roles)
                    },
            )
    }
}

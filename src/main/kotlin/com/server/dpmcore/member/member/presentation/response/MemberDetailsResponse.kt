package com.server.dpmcore.member.member.presentation.response

import com.server.dpmcore.member.member.domain.model.Member
import io.swagger.v3.oas.annotations.media.Schema

data class MemberDetailsResponse(
    @field:Schema(
        description = "이메일",
        example = "depromeetcore@gmail.com",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val email: String,
    @field:Schema(
        description = "이름",
        example = "디프만",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val name: String?,
    @field:Schema(
        description = "파트",
        example = "WEB",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val part: String?,
    @field:Schema(
        description = "기수",
        example = "17",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val cohort: String?,
    @field:Schema(
        description = "어드민 여부",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isAdmin: Boolean,
) {
    companion object {
        fun of(
            member: Member,
            roles: List<String>,
        ): MemberDetailsResponse =
            MemberDetailsResponse(
                email = member.email,
                name = member.name,
                part = member.part?.name,
                cohort = "17",
                isAdmin = roles.contains(ADMIN_ROLE),
            )

        private const val ADMIN_ROLE = "ORGANIZER"
    }
}

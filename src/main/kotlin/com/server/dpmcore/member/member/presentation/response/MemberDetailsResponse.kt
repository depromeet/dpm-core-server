package com.server.dpmcore.member.member.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.server.dpmcore.member.member.domain.model.Member
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MemberDetailsResponse(
    // TODO: 디자인 파트와 논의 후 수정 필요
    @field:Schema(
        description = "이메일",
        example = "depromeetcore@gmail.com",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val email: String,
    @field:Schema(
        description = "이름",
        example = "디프만",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val name: String?,
) {
    companion object {
        fun from(member: Member): MemberDetailsResponse =
            MemberDetailsResponse(
                email = member.email,
                name = member.name,
            )
    }
}

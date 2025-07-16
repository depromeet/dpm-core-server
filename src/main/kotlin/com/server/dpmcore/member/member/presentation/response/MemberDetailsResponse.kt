package com.server.dpmcore.member.member.presentation.response

import com.server.dpmcore.member.member.domain.model.Member

data class MemberDetailsResponse(
    // TODO: 디자인 파트와 논의 후 수정 필요
    val email: String,
    val name: String,
) {
    companion object {
        fun from(member: Member): MemberDetailsResponse =
            MemberDetailsResponse(
                email = member.email,
                name = member.name ?: "",
            )
    }
}

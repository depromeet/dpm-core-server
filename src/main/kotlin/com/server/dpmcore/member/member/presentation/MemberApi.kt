package com.server.dpmcore.member.member.presentation

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.presentation.response.MemberDetailsResponse
import com.server.dpmcore.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "멤버 API")
interface MemberApi {
    fun me(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<MemberDetailsResponse>
}

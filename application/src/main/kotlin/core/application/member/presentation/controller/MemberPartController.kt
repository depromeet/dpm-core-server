package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.response.MemberPartsResponse
import core.domain.member.enums.MemberPart
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MemberPartController : MemberPartApi {
    @PreAuthorize("hasAuthority('update:attendance')")
    @GetMapping("/v3/members/parts")
    override fun getParts(): CustomResponse<MemberPartsResponse> =
        CustomResponse.ok(MemberPartsResponse(MemberPart.entries.map { it.name } + UNASSIGNED))

    companion object {
        /** 파트가 없는(null) 멤버를 고르는 필터 값. MemberPart 로 저장하지 않는다 */
        private const val UNASSIGNED = "UNASSIGNED"
    }
}

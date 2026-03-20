package core.application.coreDev.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.coreDev.application.service.CoreDevMemberQueryService
import core.application.coreDev.presentation.response.CoreDevMemberDetailResponse
import core.application.coreDev.presentation.response.CoreDevMemberListResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.member.vo.MemberId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/core/dev")
class CoreDevQueryController(
    private val coreDevMemberQueryService: CoreDevMemberQueryService,
) : CoreDevQueryApi {
    @PreAuthorize("hasAuthority('read:core')")
    @GetMapping("/members")
    override fun allMember(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<CoreDevMemberListResponse> {
        return CustomResponse.ok(coreDevMemberQueryService.getAllMembers())
    }

    @PreAuthorize("hasAuthority('read:core')")
    @GetMapping("/members/{memberEmail}")
    override fun memberInfo(
        @CurrentMemberId memberId: MemberId,
        @RequestParam memberEmail: String,
    ): CustomResponse<CoreDevMemberDetailResponse> =
        CustomResponse.ok(
            coreDevMemberQueryService.getEmailMember(memberEmail),
        )
}

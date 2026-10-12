package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.common.exception.GlobalExceptionCode
import core.application.member.application.service.MemberManagementCommandService
import core.application.member.application.service.MemberManagementQueryService
import core.application.member.presentation.request.MemberManagementBulkUpdateRequest
import core.application.member.presentation.request.MemberManagementRequest
import core.application.member.presentation.request.MemberManagementUpdateRequest
import core.application.member.presentation.response.MemberManagementResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestController
@RequestMapping("/v3/members")
class MemberManagementController(
    private val queryService: MemberManagementQueryService,
    private val commandService: MemberManagementCommandService,
) : MemberManagementApi {
    @PreAuthorize("hasAuthority('read:member')")
    @GetMapping("/overview")
    override fun getOverview(
        @Valid @ModelAttribute request: MemberManagementRequest,
    ): CustomResponse<MemberManagementResponse> = CustomResponse.ok(queryService.getOverview(request))

    @PreAuthorize(
        "hasAuthority('update:member') and (#request.memberType == null or hasAuthority('update:authorization'))",
    )
    @PatchMapping("/{memberId}")
    override fun updateMember(
        @PathVariable memberId: Long,
        @Valid @RequestBody request: MemberManagementUpdateRequest,
    ): CustomResponse<Void> {
        commandService.update(memberId, request)
        return CustomResponse.ok()
    }

    @PreAuthorize(
        "hasAuthority('update:member') and " +
            "(#request.changes.memberType == null or hasAuthority('update:authorization'))",
    )
    @PatchMapping("/bulk")
    override fun updateMembers(
        @Valid @RequestBody request: MemberManagementBulkUpdateRequest,
    ): CustomResponse<Void> {
        commandService.updateBulk(request)
        return CustomResponse.ok()
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleTypeMismatch(): CustomResponse<Void> = CustomResponse.error(GlobalExceptionCode.INVALID_INPUT)
}

package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.common.exception.GlobalExceptionCode
import core.application.member.application.exception.MemberManagementNotImplementedException
import core.application.member.presentation.request.MemberMergeRequest
import core.application.security.annotation.CurrentMemberId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestController
@RequestMapping("/v3/members")
class MemberAdmissionController : MemberAdmissionApi {
    @PreAuthorize("hasAuthority('create:member') and hasAuthority('delete:member')")
    @PostMapping("/merge")
    override fun mergeAndApprove(
        @Valid @RequestBody request: MemberMergeRequest,
    ): CustomResponse<Void> = throw MemberManagementNotImplementedException()

    @PreAuthorize("hasAuthority('create:member')")
    @PostMapping("/{memberId}/rejection")
    override fun reject(
        @PathVariable memberId: Long,
    ): CustomResponse<Void> = throw MemberManagementNotImplementedException()

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/reapplication")
    override fun reapply(
        @CurrentMemberId memberId: Long,
    ): CustomResponse<Void> = throw MemberManagementNotImplementedException()

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleTypeMismatch(): CustomResponse<Void> = CustomResponse.error(GlobalExceptionCode.INVALID_INPUT)
}

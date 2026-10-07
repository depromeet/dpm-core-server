package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.common.exception.GlobalExceptionCode
import core.application.member.application.exception.MemberManagementNotImplementedException
import core.application.member.presentation.request.MemberBulkDeleteRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestController
@RequestMapping("/v3/members")
class MemberDeletionController : MemberDeletionApi {
    @PreAuthorize("hasAuthority('delete:member')")
    @DeleteMapping("/{memberId}/hard-delete")
    override fun delete(
        @PathVariable memberId: Long,
    ): CustomResponse<Void> = throw MemberManagementNotImplementedException()

    @PreAuthorize("hasAuthority('delete:member')")
    @DeleteMapping("/hard-delete/bulk")
    override fun deleteBulk(
        @Valid @RequestBody request: MemberBulkDeleteRequest,
    ): CustomResponse<Void> = throw MemberManagementNotImplementedException()

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleTypeMismatch(): CustomResponse<Void> = CustomResponse.error(GlobalExceptionCode.INVALID_INPUT)
}

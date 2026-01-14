package core.application.bill.presentation.controller.account

import core.application.bill.application.service.account.BillAccountQueryService
import core.application.bill.presentation.mapper.account.BillAccountMapper.toBillAccountResponse
import core.application.bill.presentation.response.account.BillAccountResponse
import core.application.common.exception.CustomResponse
import core.domain.bill.vo.BillAccountId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/bills/accounts")
class BillAccountQueryController(
    private val billAccountQueryService: BillAccountQueryService,
) : BillAccountQueryApi {
    // TODO: BillAccount가 Bill 애그리거트의 하위 도메인이라 API를 애그리거트 루트 단으로 올리는 것에 대하여 논의 필요
    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/{billAccountId}")
    override fun getBillAccount(
        @PathVariable("billAccountId")
        billAccountId: Long,
    ): CustomResponse<BillAccountResponse> {
        val billAccount = billAccountQueryService.findBy(BillAccountId(billAccountId))
        return CustomResponse.ok(
            toBillAccountResponse(billAccount),
        )
    }
}

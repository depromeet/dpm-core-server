package core.application.bill.presentation.request

import core.domain.bill.port.inbound.command.UpdateMemberListDepositCommand
import core.domain.bill.port.inbound.command.UpdateMemberListDepositMemberCommand
import core.domain.bill.vo.BillId
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class UpdateMemberListDepositRequest(
    @NotEmpty
    val members: List<UpdateMemberDepositRequest>,
) {
    data class UpdateMemberDepositRequest(
        @field:Schema(
            description = "멤버 식별자",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val memberId: MemberId,
        @field:Schema(
            description = "입금 완료 여부",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val isDeposit: Boolean,
    )

    fun toCommand(billId: BillId) =
        UpdateMemberListDepositCommand(
            billId = billId,
            members =
                members.map {
                    UpdateMemberListDepositMemberCommand(
                        memberId = it.memberId,
                        isDeposit = it.isDeposit,
                    )
                },
        )
}

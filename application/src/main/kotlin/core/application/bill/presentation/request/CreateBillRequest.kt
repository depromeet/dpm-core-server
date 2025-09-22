package core.application.bill.presentation.request

import core.domain.authority.vo.AuthorityId
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class CreateBillRequest(
    @field:Schema(
        description = "정산 제목",
        example = "OT 회식 정산",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val title: String,
    @field:Schema(
        description = "정산 설명",
        example = "OT 회식에 대한 정산 내역입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val description: String?,
    @field:Schema(
        description = "정산 계좌 일련번호(지금은 1번 무조건 보내면 됩니다.)",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val billAccountId: Long,
    @field:Schema(
        description = "정산에 초대할 대상 그룹의 ID 목록(지금은 1(17기 디퍼), 2(17기 운영진)만 보내면됩니다.)",
        example = "[1, 2]",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotEmpty
    @field:Size(min = 1)
    val invitedAuthorityIds: MutableList<AuthorityId>,
    @field:NotEmpty
    @field:Size(min = 1)
    val gatherings: MutableList<GatheringForBillCreateRequest>,
)

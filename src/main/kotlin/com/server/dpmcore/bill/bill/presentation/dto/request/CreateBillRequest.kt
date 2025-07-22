package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.authority.domain.model.AuthorityId
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class CreateBillRequest(
    val title: String,
    val description: String?,
    val hostUserId: Long,
    val billAccountId: Long,
    @field:NotEmpty @field:Size(min = 1) val invitedAuthorityIds: MutableList<AuthorityId>,
    @field:NotEmpty @field:Size(min = 1) val gatherings: MutableList<GatheringForBillCreateRequest>,
)

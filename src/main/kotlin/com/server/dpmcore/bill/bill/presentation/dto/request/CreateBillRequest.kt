package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.authority.domain.model.AuthorityId
import jakarta.validation.constraints.NotEmpty

data class CreateBillRequest(
    val title: String,
    val description: String?,
    val hostUserId: Long,
    val billAccountId: Long,
    val invitedAuthorityIds: List<AuthorityId>,
    @NotEmpty val gatherings: MutableList<GatheringForBillCreateRequest>,
)

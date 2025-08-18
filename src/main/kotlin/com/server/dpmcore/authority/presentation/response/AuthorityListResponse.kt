package com.server.dpmcore.authority.presentation.response

import com.server.dpmcore.authority.domain.model.Authority
import io.swagger.v3.oas.annotations.media.Schema

data class AuthorityListResponse(
    @field:Schema(
        description = "권한 목록",
        example = """
            [
                {
                    "id": 1,
                    "name": "17_DEEPER"
                },
                {
                    "id": 2,
                    "name": "17_ORGANIZER"
                }
            ]
        """,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val authorities: List<AuthorityResponse>,
) {
    data class AuthorityResponse(
        @field:Schema(
            description = "권한 식별자",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val id: Long,
        @field:Schema(
            description = "권한 이름",
            example = "17_DEEPER",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val name: String,
    )

    companion object {
        fun from(authorities: List<Authority>): AuthorityListResponse =
            AuthorityListResponse(
                authorities =
                    authorities.map { authority ->
                        AuthorityResponse(
                            id = authority.id?.value ?: 0L,
                            name = authority.name,
                        )
                    },
            )
    }
}

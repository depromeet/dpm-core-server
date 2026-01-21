package com.server.dpmcore.authority.presentation.response

import com.server.dpmcore.authority.domain.model.Authority

data class AuthorityListResponse(
    val authorities: List<AuthorityResponse>,
) {
    data class AuthorityResponse(
        val id: Long,
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

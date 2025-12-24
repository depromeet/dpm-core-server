package core.application.authorization.presentation.resposne

import core.domain.authorization.aggregate.Role
import io.swagger.v3.oas.annotations.media.Schema

data class RoleListResponse(
    val roles: List<RoleResponse>,
) {
    data class RoleResponse(
        @field:Schema(
            description = "역할 식별자",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val id: Long,
        @field:Schema(
            description = "역할 이름",
            example = "17기 디퍼",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val name: String,
    )

    companion object {
        fun from(roles: List<Role>): RoleListResponse =
            RoleListResponse(
                roles =
                    roles.map { role ->
                        RoleResponse(
                            id = role.id?.value ?: 0L,
                            name = role.name,
                        )
                    },
            )
    }
}

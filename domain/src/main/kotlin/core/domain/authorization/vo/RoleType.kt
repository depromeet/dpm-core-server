package core.domain.authorization.vo

sealed class RoleType(
    val code: String,
    val aliases: Set<String>,
) {

    data object Core : RoleType(
        code = "CORE",
        aliases = setOf("코어", "core")
    )

    data object Organizer : RoleType(
        code = "ORGANIZER",
        aliases = setOf("운영진", "운영", "관리자")
    )

    data object Deeper : RoleType(
        code = "DEEPER",
        aliases = setOf("디퍼", "deeper")
    )

    data object Guest : RoleType(
        code = "GUEST",
        aliases = emptySet()
    )

    companion object {
        private val values: List<RoleType> by lazy {
            RoleType::class.sealedSubclasses
                .mapNotNull { it.objectInstance }
                .filter { it != Guest }
        }

        fun from(raw: String?): RoleType {
            if (raw.isNullOrBlank()) return Guest

            val tokens = raw
                .lowercase()
                .split(Regex("[^가-힣a-z]+"))
                .filter { it.isNotBlank() }

            return values.firstOrNull { role ->
                role.aliases.any { alias ->
                    tokens.any { token ->
                        token == alias.lowercase()
                    }
                }
            } ?: Guest
        }
    }

}

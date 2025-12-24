package core.domain.authorization.vo

sealed class RoleType(
    private val keywords: Set<String>,
) {

    data object Organizer : RoleType(
        keywords = setOf("운영진", "운영", "관리자")
    )

    data object Core : RoleType(
        keywords = setOf("코어", "core", "CORE")
    )

    data object Deeper : RoleType(
        keywords = setOf("디퍼", "deeper", "DEEPER")
    )

    data object Guest : RoleType(
        keywords = emptySet()
    )

    fun matches(roleName: String): Boolean =
        keywords.any { roleName.contains(it) }

    companion object {

        private val predefined = listOf(
            Organizer,
            Core,
            Deeper,
        )

        fun from(roleName: String?): RoleType {
            if (roleName.isNullOrBlank()) return Guest

            return predefined.firstOrNull { it.matches(roleName) }
                ?: Guest
        }
    }
}

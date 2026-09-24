package core.domain.authorization.vo

sealed class RoleType(
    val code: String,
) {
    data object Master : RoleType("MASTER")
    data object Core : RoleType("CORE")
    data object Organizer : RoleType("ORGANIZER")
    data object Deeper : RoleType("DEEPER")
    data object Guest : RoleType("GUEST")

    companion object {
        private val values: List<RoleType> by lazy {
            RoleType::class.sealedSubclasses.mapNotNull { it.objectInstance }
        }

        fun fromCode(raw: String?): RoleType =
            values.firstOrNull { it.code.equals(raw?.trim(), ignoreCase = true) } ?: Guest

        fun from(raw: String?): RoleType {
            if (raw.isNullOrBlank()) return Guest
            fromCode(raw).takeIf { it != Guest }?.let { return it }
            val tokens = raw.lowercase().split(Regex("[^가-힣a-z]+")).filter { it.isNotBlank() }
            val aliasMap = mapOf(
                "master" to Master, "마스터" to Master,
                "core" to Core, "코어" to Core,
                "organizer" to Organizer, "운영진" to Organizer, "운영" to Organizer, "관리자" to Organizer,
                "deeper" to Deeper, "디퍼" to Deeper,
            )
            return tokens.firstNotNullOfOrNull { aliasMap[it] } ?: Guest
        }
    }
}

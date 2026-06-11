package core.application.member.application.service

import org.springframework.stereotype.Component

@Component
class MemberNameHashTypeValidator {
    fun isHashType(name: String): Boolean {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return false
        }

        return UUID_REGEX.matches(normalizedName) || HASH_LIKE_REGEX.matches(normalizedName)
    }

    companion object {
        private val UUID_REGEX =
            Regex(
                pattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            )
        private val HASH_LIKE_REGEX = Regex("^[0-9a-fA-F]{16,64}$")
    }
}

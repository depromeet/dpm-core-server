package core.domain.authorization.vo

/**
 * canonical role name + cohort_id 조합을 사람이 읽기 좋은 표시 문자열로 변환한다.
 *
 * 규칙 (팀 컨벤션):
 * - MASTER               → "마스터"
 * - CORE + cohort N      → "${N}기 코어"
 * - ORGANIZER + cohort N → "${N}기 운영진"
 * - DEEPER + cohort N    → "${N}기 디퍼"
 * - GUEST                → "게스트"
 * - cohort 매칭 불가      → 원본 canonical name 그대로 (fallback)
 */
object RoleDisplayName {
    fun of(
        roleName: String?,
        cohortValue: Long?,
    ): String {
        val roleType = RoleType.from(roleName)
        return when (roleType) {
            RoleType.Master -> "마스터"
            RoleType.Guest -> "게스트"
            RoleType.Organizer -> cohortValue?.let { "${it}기 운영진" } ?: roleType.code
            RoleType.Deeper -> cohortValue?.let { "${it}기 디퍼" } ?: roleType.code
            RoleType.Core -> cohortValue?.let { "${it}기 코어" } ?: roleType.code
        }
    }
}

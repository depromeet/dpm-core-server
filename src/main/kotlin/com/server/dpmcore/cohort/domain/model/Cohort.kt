package com.server.dpmcore.cohort.domain.model

import java.time.LocalDateTime

/**
 * 기수(Cohort)를 표현하는 도메인 모델입니다.
 *
 * 기수는 조직 또는 활동 주기의 단위를 의미하며, 회원이 어느 시점의 활동에 소속되어 있었는지를 나타냅니다.
 * 예를 들어 일반 활동 기수는 "16기", "17기"와 같은 형식으로, 코어 기수는 "코어 1기"로 저장됩니다.
 *
 * 기수 값은 문자열로 저장되며, 각 기수는 고유하고 중복될 수 없습니다.
 * 한 명의 회원은 여러 기수를 수료할 수 있으며, 활동 이력으로서 다수의 기수에 소속될 수 있습니다.
 *
 * equals와 hashCode 구현 규칙:
 * - equals는 동일성 판단을 위해 id만 비교합니다.
 * - hashCode는 equals와 일치하도록 id의 해시코드를 반환해야 합니다.
 *
 */
data class Cohort(
    val id: CohortId = CohortId.generate(),
    val value: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cohort

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

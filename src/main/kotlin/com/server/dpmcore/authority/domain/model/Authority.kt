package com.server.dpmcore.authority.domain.model

import java.time.LocalDateTime

/**
 * 권한(Authority)을 표현하는 도메인 모델입니다.
 *
 * 이 모델은 시스템 내 사용자에게 부여되는 역할 또는 직책을 나타냅니다.
 * 각 권한은 기능 접근 제어, 승인 권한, 페이지 접근 등 다양한 정책에 활용될 수 있습니다.
 *
 * 한 명의 회원은 여러 개의 권한을 동시에 가질 수 있으며,
 * 부여된 권한은 사용자에게 특정 기능 사용 또는 관리 권한을 부여하는 기준이 됩니다.
 *
 * 예를 들어 다음과 같은 권한이 존재할 수 있습니다:
 * - 회장
 * - 부회장
 * - 운영진
 * - 디퍼(일반 부원)
 *
 * 추후 필요에 따라 권한은 자유롭게 추가될 수 있습니다.
 *
 * equals와 hashCode 구현 규칙:
 * - equals는 동일성 판단을 위해 id만 비교합니다.
 * - hashCode는 equals와 일치하도록 id의 해시코드를 반환해야 합니다.
 *
 */
data class Authority(
    val id: AuthorityId? = null,
    val name: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Authority

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

package com.server.dpmcore.authority.domain.model

import com.server.dpmcore.member.memberAuthority.domain.MemberAuthorityId

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
 * - equals는 동일성 판단을 위해 핵심 필드인 id와 name을 함께 비교합니다.
 * - 두 Authority 객체의 id와 name이 모두 같을 때 동등한 것으로 간주합니다.
 * - hashCode는 id와 name을 조합하여 계산하며, equals 규칙과 일관성을 유지합니다.
 *
 */
class Authority(
    val id: AuthorityId? = null,
    val name: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val memberAuthorityIds: List<MemberAuthorityId> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Authority) return false

        return id == other.id && name == other.name
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "Authority(id=$id, name='$name', createdAt=$createdAt, updatedAt=$updatedAt, " +
            "memberAuthorityIds=$memberAuthorityIds)"
    }
}

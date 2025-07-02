package com.server.dpmcore.member.member.domain.model

import com.server.dpmcore.member.memberAuthority.domain.MemberAuthority
import com.server.dpmcore.member.memberCohort.domain.MemberCohort
import com.server.dpmcore.member.memberTeam.domain.MemberTeam

/**
 * 회원(Member)을 표현하는 도메인 모델이며, 애그리거트 루트입니다.
 *
 * 이 도메인은 시스템 내 사용자 정보를 나타내며,
 * 권한, 기수, 팀, OAuth 정보 등 하위 도메인 객체들의 루트로 동작합니다.
 * 외부에서는 해당 루트를 통해서만 관련 도메인 객체에 접근하거나 변경할 수 있습니다.
 *
 * 이메일은 소셜 로그인(예: Kakao)에서 제공받은 이메일 주소입니다.
 *
 * equals와 hashCode 구현 규칙:
 * - equals는 동일성 판단을 위해 핵심 필드인 id, name, email, part를 함께 비교합니다.
 * - 두 Member 객체의 id, name, email, part가 모두 같을 때 동등한 것으로 간주합니다.
 * - hashCode는 id, name, email, part를 조합하여 계산하며, equals 규칙과 일관성을 유지합니다.
 *
 */
data class Member(
    val id: MemberId? = null,
    val name: String,
    val email: String,
    val part: MemberPart,
    val status: MemberStatus,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
    val memberAuthorities: List<MemberAuthority> = emptyList(),
    val memberCohorts: List<MemberCohort> = emptyList(),
    val memberTeams: List<MemberTeam> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Member) return false

        return id == other.id &&
            name == other.name &&
            email == other.email &&
            part == other.part
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + part.hashCode()
        return result
    }
}

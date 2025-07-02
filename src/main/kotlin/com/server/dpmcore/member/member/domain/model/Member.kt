package com.server.dpmcore.member.member.domain.model

import java.time.LocalDateTime

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
 * - equals는 동일성 판단을 위해 id만 비교합니다.
 * - hashCode는 equals와 일치하도록 id의 해시코드를 반환해야 합니다.
 *
 */
data class Member(
    val id: MemberId = MemberId.generate(),
    val name: String,
    val email: String,
    val part: MemberPart,
    val status: MemberStatus,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Member

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

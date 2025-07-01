package com.server.dpmcore.member.member.domain

import java.time.LocalDateTime

/**
 * 회원(Member)을 표현하는 도메인 모델이며, 애그리거트 루트입니다.
 *
 * 이 도메인은 시스템 내 사용자 정보를 나타내며,
 * 권한, 기수, 팀, OAuth 정보 등 하위 도메인 객체들의 루트로 동작합니다.
 * 외부에서는 해당 루트를 통해서만 관련 도메인 객체에 접근하거나 변경할 수 있습니다.
 *
 * 이메일은 소셜 로그인(예: Kakao)에서 제공받은 이메일 주소입니다.
 */
data class Member(
    val id: MemberId,
    val name: String,
    val email: String,
    val part: MemberPart,
    val status: MemberStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

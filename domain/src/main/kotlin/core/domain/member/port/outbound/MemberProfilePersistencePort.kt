package core.domain.member.port.outbound

import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import java.time.Instant

interface MemberProfilePersistencePort {
    fun findProfile(memberId: Long): MemberProfile?

    /** 회원 행을 먼저 잠그고 최신 프로필을 반환한다. 쓰기 트랜잭션 안에서 호출한다. */
    fun lockProfile(memberId: Long): MemberProfile?

    /** 잠근 회원의 프로필을 갱신한다. 완료 후에는 가입 대기 회원만 변경하며 최초 완료 시각은 유지한다. */
    fun completeProfile(
        memberId: Long,
        name: String,
        part: MemberPart,
    ): Boolean

    /** 반려 처리 중 잠근 회원의 완료 표시만 해제하고 기존 입력 값은 보존한다. */
    fun resetCompletion(memberId: Long)

    fun hasAppleAccount(memberId: Long): Boolean
}

data class MemberProfile(
    val memberId: Long,
    val name: String,
    val part: MemberPart?,
    val status: MemberStatus,
    val deletedAt: Instant?,
    val completedAt: Instant?,
)

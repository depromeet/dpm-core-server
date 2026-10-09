package core.domain.member.port.outbound

import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import java.time.Instant

interface MemberProfilePersistencePort {
    fun findProfile(memberId: Long): MemberProfile?

    /** 회원 행을 먼저 잠그고 최신 프로필을 반환한다. 쓰기 트랜잭션 안에서 호출한다. */
    fun lockProfile(memberId: Long): MemberProfile?

    /** 잠근 회원의 프로필만 갱신한다. 완료한 프로필은 다시 변경하지 않는다. */
    fun completeProfile(memberId: Long, name: String, part: MemberPart): Boolean

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

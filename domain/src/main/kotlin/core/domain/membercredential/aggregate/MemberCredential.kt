package core.domain.membercredential.aggregate

import core.domain.member.vo.MemberId
import java.time.Instant

/**
 * 이메일 비밀번호 인증을 위한 별도 자격 증명 도메인입니다.
 *
 * 이 도메인은 기존 Member 도메인과 분리되어 있어,
 * 추후 비밀번호 로그인 기능을 제거할 때 쉽게 삭제할 수 있습니다.
 * Member와는 memberId를 통해 연결됩니다.
 */
class MemberCredential(
    val id: MemberCredentialId? = null,
    val memberId: MemberId,
    val email: String,
    val password: String,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    companion object {
        fun create(
            memberId: MemberId,
            email: String,
            encodedPassword: String,
        ): MemberCredential {
            val now = Instant.now()
            return MemberCredential(
                id = null,
                memberId = memberId,
                email = email,
                password = encodedPassword,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}

@JvmInline
value class MemberCredentialId(val value: Long)

package core.entity.membercredential

import core.domain.member.vo.MemberId
import core.domain.membercredential.aggregate.MemberCredential
import core.domain.membercredential.aggregate.MemberCredentialId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

/**
 * 회원 자격 증명(이메일 비밀번호)을 저장하는 별도의 엔티티입니다.
 *
 * Member 엔티티와는 분리되어 있어, 추후 비밀번호 로그인 기능을 제거할 때
 * 이 테이블과 관련 코드만 삭제하면 됩니다.
 */
@Entity
@Table(name = "member_credentials")
class MemberCredentialEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(nullable = false, unique = true)
    var email: String,
    @Column(nullable = false)
    var password: String,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,
    @Column(name = "updated_at")
    var updatedAt: Instant? = null,
) {
    fun toDomain(): MemberCredential =
        MemberCredential(
            id = MemberCredentialId(this.id),
            memberId = MemberId(this.memberId),
            email = email,
            password = password,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun from(domain: MemberCredential): MemberCredentialEntity =
            MemberCredentialEntity(
                id = domain.id?.value ?: 0L,
                memberId = domain.memberId.value,
                email = domain.email,
                password = domain.password,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}

package core.entity.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "member_oauth_link_requests")
class MemberOAuthLinkRequestEntity(
    @Id @Column(name = "state_hash", length = 64) val stateHash: String,
    @Column(name = "member_id", nullable = false) val memberId: Long,
    @Column(nullable = false, length = 20) val provider: String,
    @Column(name = "redirect_uri", nullable = false, length = 2048) val redirectUri: String,
    @Column(name = "code_verifier", nullable = false, length = 128) val codeVerifier: String,
    @Column(name = "expires_at", nullable = false) val expiresAt: Instant,
)

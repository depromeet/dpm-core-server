package core.application.refreshToken.application.support

import java.security.MessageDigest

/**
 * 리프레시 토큰을 DB 조회 키로 쓰기 위한 해시.
 *
 * MySQL 의 `SHA2(token, 256)` 과 바이트 단위로 동일한 64자 소문자 hex 를 만든다.
 * 기존 행을 `UPDATE refresh_tokens SET token_hash = SHA2(token, 256)` 으로 백필하므로
 * 이 동등성이 깨지면 마이그레이션된 세션이 전부 조회에 실패한다.
 *
 * @author junwon
 * @since 2026.08.25
 */
object TokenHasher {
    fun sha256Hex(token: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}

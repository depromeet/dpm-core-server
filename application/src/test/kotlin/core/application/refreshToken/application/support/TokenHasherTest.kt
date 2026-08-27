package core.application.refreshToken.application.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * 마이그레이션 백필이 `UPDATE refresh_tokens SET token_hash = SHA2(token, 256)` 이므로,
 * 이 해시 형식이 MySQL 과 어긋나면 기존 세션이 전부 조회에 실패한다.
 * 기대값은 MySQL 8 에서 직접 실행한 결과다.
 */
class TokenHasherTest {
    @Test
    fun `해시 형식이 MySQL SHA2와 일치한다`() {
        // SELECT SHA2('abc', 256);
        assertThat(TokenHasher.sha256Hex("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")

        // SELECT SHA2('', 256);
        assertThat(TokenHasher.sha256Hex(""))
            .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    }

    @Test
    fun `해시는 항상 64자 소문자 hex다`() {
        val hash = TokenHasher.sha256Hex("eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIzMzUifQ.signature")

        assertThat(hash).hasSize(64)
        assertThat(hash).matches("[0-9a-f]{64}")
    }

    @Test
    fun `같은 입력은 같은 해시를 만든다`() {
        assertThat(TokenHasher.sha256Hex("token")).isEqualTo(TokenHasher.sha256Hex("token"))
        assertThat(TokenHasher.sha256Hex("token")).isNotEqualTo(TokenHasher.sha256Hex("token2"))
    }
}

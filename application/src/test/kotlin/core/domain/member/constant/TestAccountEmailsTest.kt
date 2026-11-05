package core.domain.member.constant

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestAccountEmailsTest {
    @Test
    fun `suffix match is case insensitive and ignores null or other domains`() {
        assertThat(TestAccountEmails.matches(null)).isFalse()
        assertThat(TestAccountEmails.matches("user@example.com")).isFalse()
        assertThat(TestAccountEmails.matches("user@dpm.test")).isTrue()
        assertThat(TestAccountEmails.matches("QA@DPM.TEST")).isTrue()
        assertThat(TestAccountEmails.matches("not-dpm.test")).isFalse()
    }
}

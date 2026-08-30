package core.application.common.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.io.ClassPathResource
import javax.xml.parsers.DocumentBuilderFactory

/**
 * logback-spring.xml 의 `source` 와 application.yml 의 키는 문자열로만 연결돼 있다.
 * 한쪽 이름만 바뀌면 값이 비어 들어가고, 애플리케이션은 아무 일 없다는 듯 뜨면서 알림만 사라진다.
 * 배포 후에야 드러나는 실수라 설정 파일 자체를 대조한다.
 */
class DiscordConfigurationKeysTest {
    @Test
    fun `logback 이 참조하는 설정 키가 모두 yml 에 정의돼 있다`() {
        val referenced = springPropertySources() - RUNTIME_PROVIDED
        val defined = propertyNames("application.yml") + propertyNames("application-dev.yml")

        assertThat(referenced).isNotEmpty()
        assertThat(defined).containsAll(referenced)
    }

    @Test
    fun `프로필별 webhook 주소가 서로 다른 환경 변수를 읽는다`() {
        val local = propertyValue("application-local.yml", WEBHOOK_KEY)
        val dev = propertyValue("application-dev.yml", WEBHOOK_KEY)
        val prod = propertyValue("application-prod.yml", WEBHOOK_KEY)

        assertThat(listOf(local, dev, prod)).doesNotContainNull().doesNotHaveDuplicates()
        // 기본값을 두지 않기로 했다. 값이 없으면 기동이 실패해야 한다.
        assertThat(listOf(local, dev, prod)).allSatisfy { assertThat(it).doesNotContain(":") }
    }

    @Test
    fun `공통 설정에는 webhook 주소를 두지 않는다`() {
        // 공통에 두면 프로필별로 다른 채널을 쓸 수 없고, 로컬에서 prod 채널로 새어나갈 수 있다
        assertThat(propertyNames("application.yml")).doesNotContain(WEBHOOK_KEY)
    }

    private fun springPropertySources(): Set<String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ClassPathResource("logback-spring.xml").inputStream)

        val nodes = document.getElementsByTagName("springProperty")
        return (0 until nodes.length)
            .mapNotNull { nodes.item(it).attributes.getNamedItem("source")?.nodeValue }
            .toSet()
    }

    private fun propertyNames(fileName: String): Set<String> =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .filterIsInstance<EnumerablePropertySource<*>>()
            .flatMap { it.propertyNames.toList() }
            .toSet()

    private fun propertyValue(
        fileName: String,
        key: String,
    ): String? =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .firstNotNullOfOrNull { it.getProperty(key) }
            ?.toString()

    companion object {
        private const val WEBHOOK_KEY = "logging.discord.webhook-url"

        /** 스프링이 런타임에 채우는 값이라 yml 에 없다. */
        private val RUNTIME_PROVIDED = setOf("spring.profiles.active")
    }
}

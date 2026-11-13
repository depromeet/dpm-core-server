package core.application.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 보안 설정은 v1·v2·v3 API 경로 전체를 URL 단계에서 허용하므로 메서드 권한이 유일한 방어선이다.
 * 권한 검사가 없는 API가 새로 생기면 실패한다. 의도한 공개 API는 [PUBLIC_ENDPOINTS]에 이유와 함께 둔다.
 */
class EndpointAuthorizationCoverageTest {
    private fun unguardedEndpoints(): Set<String> {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(RestController::class.java))
        return scanner.findCandidateComponents("core.application")
            .map { Class.forName(it.beanClassName) }
            .flatMap { controller ->
                val classGuard = AnnotatedElementUtils.findMergedAnnotation(controller, PreAuthorize::class.java)
                val prefix =
                    AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping::class.java)?.path?.firstOrNull().orEmpty()
                controller.declaredMethods
                    .filterNot { it.isSynthetic || it.isBridge }
                    .mapNotNull { method ->
                        val mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java)
                        val guard = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize::class.java) ?: classGuard
                        if (mapping == null || guard != null) return@mapNotNull null
                        "${mapping.method.joinToString(",")} $prefix${mapping.path.firstOrNull().orEmpty()}"
                    }
            }.toSet()
    }

    @Test
    fun `모든 API는 메서드 권한을 선언하거나 의도한 공개 API 목록에 있어야 한다`() {
        assertThat(unguardedEndpoints()).containsExactlyInAnyOrderElementsOf(PUBLIC_ENDPOINTS)
    }

    private companion object {
        /** 로그인 전에 호출해야 하는 인증 진입점과 토큰 재발급. */
        val PUBLIC_ENDPOINTS =
            setOf(
                "POST /login/email",
                "POST /login/auth/apple",
                "POST /login/auth/kakao",
                "POST /login/auth/kakao/tokens",
                "GET /login/oauth/authorization/{provider}",
                "POST /v1/members/login/auth/apple",
                "POST /v1/auth/kakao/native",
                "GET /v1/reissue",
            )
    }
}

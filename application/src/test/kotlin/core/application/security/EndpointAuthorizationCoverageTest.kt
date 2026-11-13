package core.application.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

/**
 * 보안 설정은 v1·v2·v3 API 경로 전체를 URL 단계에서 허용하므로 메서드 권한이 유일한 방어선이다.
 * 권한 검사가 없거나 permitAll()처럼 누구나 통과하는 API가 새로 생기면 실패한다. 의도한 공개 API는 [PUBLIC_ENDPOINTS]에 둔다.
 * 로그인 경로(/login 하위)도 URL 단계에서 허용된다. CI는 컴파일만 하므로 이 검사는 로컬 테스트 실행 시 동작한다.
 */
class EndpointAuthorizationCoverageTest {
    private fun unguardedEndpoints(): Set<String> {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        // @RestController와 @Controller + @ResponseBody 모두 잡도록 @Controller 메타 애너테이션 기준으로 찾는다.
        scanner.addIncludeFilter(AnnotationTypeFilter(Controller::class.java))
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
                        if (mapping == null || isRestricted(guard?.value)) return@mapNotNull null
                        val paths = mapping.path.ifEmpty { arrayOf("") }
                        paths.map { "${mapping.method.joinToString(",")} $prefix$it" }
                    }.flatten()
            }.toSet()
    }

    /** 권한이 없거나 permitAll()·부정 조건처럼 사실상 누구나 통과하는 선언은 공개로 본다. */
    private fun isRestricted(expression: String?): Boolean {
        val value = expression?.trim() ?: return false
        return value != "permitAll()" && !value.startsWith("!")
    }

    @Test
    fun `모든 API는 메서드 권한을 선언하거나 의도한 공개 API 목록에 있어야 한다`() {
        assertThat(unguardedEndpoints()).containsExactlyInAnyOrderElementsOf(PUBLIC_ENDPOINTS)
    }

    private companion object {
        /** 의도한 공개 API. 추가할 때는 개인정보를 주거나 다른 회원에게 영향을 주지 않는지 확인한다. */
        val PUBLIC_ENDPOINTS =
            setOf(
                // 로그인 전에 호출하는 인증 진입점과 토큰 재발급
                "POST /login/email",
                "POST /login/auth/apple",
                "POST /login/auth/kakao",
                "POST /login/auth/kakao/tokens",
                "GET /login/oauth/authorization/{provider}",
                "GET /login/kakao",
                "GET /login/apple",
                "POST /v1/members/login/auth/apple",
                "POST /v1/auth/kakao/native",
                "GET /v1/reissue",
                // 로그인 전 화면에서 쓰는 기수 정보와 입력값만 판정하는 검증
                "GET /v1/cohort",
                "GET /v1/cohorts/latest",
                "GET /v1/cohorts",
                "POST /v1/members/name/hash-type/validation",
                // permitAll이지만 로그인 회원 ID 인자 단계에서 비로그인은 401
                "GET /v3/members/me",
                // 디프만 앱 홈이 서버 렌더링에서 토큰 없이 불러오는 활성 기수 공지 목록(회원 정보 없음)
                "GET /v1/announcements",
            )
    }
}

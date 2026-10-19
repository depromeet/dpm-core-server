package core.application.member

import core.application.member.presentation.controller.MemberController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping

/** 마이페이지 조회(me)만 /v3 로 옮기고, 나머지 경로와 메서드 권한은 그대로인지 확인한다. */
class MemberApiV3PathContractTest {
    private fun endpoints(controller: Class<*>): Set<String> {
        val prefix =
            AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping::class.java)
                ?.path
                ?.firstOrNull()
                .orEmpty()
        return controller.declaredMethods
            .filterNot { it.isSynthetic || it.isBridge }
            .mapNotNull { method ->
                val mapping =
                    AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java)
                        ?: return@mapNotNull null
                val authority =
                    AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize::class.java)?.value ?: "(none)"
                "${mapping.method.single()} $prefix${mapping.path.firstOrNull().orEmpty()} $authority"
            }.toSet()
    }

    @Test
    fun `멤버 API 는 me 만 v3 이고 나머지 경로와 권한은 그대로다`() {
        assertThat(endpoints(MemberController::class.java))
            .containsExactlyInAnyOrder(
                "POST /v1/members/name/hash-type/validation permitAll()",
                "POST /v1/members/apple/profile isAuthenticated()",
                "GET /v1/members/apple/hidden-email hasAuthority('read:member')",
                "GET /v3/members/me permitAll()",
                "GET /v1/members/overview hasAuthority('read:member')",
                "PATCH /v1/members/withdraw isAuthenticated()",
                "DELETE /v1/members/{memberId}/hard-delete hasAuthority('delete:member')",
                "PATCH /v1/members/init hasAuthority('update:member')",
                "PATCH /v1/members/whitelist hasAuthority('create:member')",
                "PATCH /v1/members/status hasAuthority('update:member')",
                "POST /v1/members/authority/cohort/init/{cohortId}/{memberId} hasAuthority('update:member')",
                "POST /v1/members/login/auth/apple (none)",
                "PATCH /v1/members/password (none)",
            )
    }
}

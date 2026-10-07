package core.application.attendance

import core.application.attendance.presentation.controller.AttendanceCommandController
import core.application.attendance.presentation.controller.AttendanceQueryController
import core.application.image.presentation.controller.ImageController
import core.application.session.presentation.controller.SessionCommandController
import core.application.session.presentation.controller.SessionQueryController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping

/**
 * 스택 PR 에서 새로 만들었거나 클라이언트가 대응해야 하는 요청·응답 변경이 있는 API 만 /v3 이고,
 * 나머지는 기존 경로와 메서드 권한 그대로인지 확인한다.
 */
class AttendanceApiV3PathContractTest {
    private fun endpoints(vararg controllers: Class<*>): Set<String> =
        controllers
            .flatMap { controller ->
                val prefix =
                    AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping::class.java)
                        ?.path
                        ?.firstOrNull()
                        .orEmpty()
                controller.declaredMethods
                    .filterNot { it.isSynthetic || it.isBridge }
                    .mapNotNull { method ->
                        val mapping =
                            AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java)
                                ?: return@mapNotNull null
                        val authority =
                            AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize::class.java)!!.value
                        "${mapping.method.single()} $prefix${mapping.path.firstOrNull().orEmpty()} $authority"
                    }
            }.toSet()

    @Test
    fun `출석 API 는 바뀐 것만 v3 이고 권한은 그대로다`() {
        assertThat(endpoints(AttendanceCommandController::class.java, AttendanceQueryController::class.java))
            .containsExactlyInAnyOrder(
                "POST /v1/sessions/{sessionId}/attendances hasAuthority('create:attendance')",
                "PATCH /v1/sessions/{sessionId}/attendances/{memberId} hasAuthority('update:attendance')",
                "PATCH /v1/sessions/{sessionId}/attendances/bulk hasAuthority('update:attendance')",
                "POST /v3/sessions/{sessionId}/absence-reasons hasAuthority('create:attendance')",
                "PATCH /v3/sessions/{sessionId}/absence-reasons hasAuthority('create:attendance')",
                "DELETE /v2/sessions/{sessionId}/absence-reasons hasAuthority('create:attendance')",
                "PATCH /v2/sessions/{sessionId}/absence-reasons/{memberId}/review hasAuthority('update:attendance')",
                "GET /v3/sessions/{sessionId}/attendances hasAuthority('update:attendance')",
                "GET /v3/members/attendances hasAuthority('create:attendance')",
                "GET /v3/sessions/{sessionId}/attendances/{memberId} hasAuthority('create:attendance')",
                "GET /v1/sessions/{sessionId}/attendances/me hasAuthority('read:attendance')",
                "GET /v3/members/{memberId}/attendances hasAuthority('update:member')",
                "GET /v3/members/me/attendances hasAuthority('read:attendance')",
                "GET /v3/sessions/{sessionId}/absence-reasons/me hasAuthority('create:attendance')",
                "GET /v3/sessions/{sessionId}/absence-reasons hasAuthority('update:attendance')",
            )
    }

    @Test
    fun `세션 API 는 바뀐 것만 v3 이고 권한은 그대로다`() {
        assertThat(endpoints(SessionCommandController::class.java, SessionQueryController::class.java))
            .containsExactlyInAnyOrder(
                "POST /v1/sessions hasAuthority('create:session')",
                "PATCH /v1/sessions hasAuthority('update:session')",
                "PATCH /v1/sessions/{sessionId}/delete hasAuthority('delete:session')",
                "GET /v1/sessions/next permitAll()",
                "GET /v1/sessions permitAll()",
                "GET /v1/sessions/{sessionId} hasAuthority('create:session')",
                "GET /v1/sessions/{sessionId}/me hasAuthority('read:session')",
                "GET /v1/sessions/{sessionId}/attendance-time hasAuthority('update:session')",
                "GET /v1/sessions/weeks hasAuthority('read:session')",
                "GET /v3/sessions/weeks hasAuthority('read:session')",
                "GET /v1/sessions/{sessionId}/update-policy hasAuthority('update:session')",
            )
    }

    @Test
    fun `이미지 API 는 v3 에만 있고 예전 v1 경로는 없다`() {
        assertThat(endpoints(ImageController::class.java))
            .containsExactlyInAnyOrder(
                "POST /v3/images/uploads isAuthenticated()",
                "POST /v3/images/uploads/{uploadId}/complete isAuthenticated()",
                "GET /v3/images/{imageId} isAuthenticated()",
                "GET /v3/images/{imageId}/download isAuthenticated()",
            )
    }
}

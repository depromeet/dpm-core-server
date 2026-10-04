package core.application.attendance

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import core.application.attendance.application.exception.InvalidAbsenceReasonImageException
import core.application.attendance.application.service.AbsenceReasonImageQueryService
import core.application.attendance.application.service.AbsenceReasonQueryService
import core.application.attendance.application.service.AttendanceQueryService
import core.application.attendance.presentation.controller.AttendanceQueryController
import core.application.attendance.presentation.request.AbsenceReportCreateRequest
import core.application.attendance.presentation.request.AbsenceReportUpdateRequest
import core.application.image.presentation.response.ImageUrlResponse
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import java.time.Instant

/** 요청 본문 의미(생략=유지, []=해제, 순서)와 운영진 이미지 URL 조회 권한·응답을 확인한다. 저장 동작은 MySQL 통합 테스트에서 본다. */
class AbsenceReasonImageContractTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `imageIds 생략과 null 은 유지, 빈 배열은 해제, 값은 순서를 지킨다`() {
        assertThat(objectMapper.readValue<AbsenceReportCreateRequest>("""{"contents":"사유"}""").toImageIds()).isNull()
        assertThat(objectMapper.readValue<AbsenceReportUpdateRequest>("""{"contents":"사유","imageIds":null}""").toImageIds()).isNull()
        assertThat(objectMapper.readValue<AbsenceReportUpdateRequest>("""{"contents":"사유","imageIds":[]}""").toImageIds()).isEmpty()
        assertThat(objectMapper.readValue<AbsenceReportCreateRequest>("""{"contents":"사유","imageIds":[3,1,2]}""").toImageIds())
            .containsExactly(ImageId(3), ImageId(1), ImageId(2))
    }

    @Test
    fun `배열 안의 null 은 400 이다`() {
        val request = objectMapper.readValue<AbsenceReportCreateRequest>("""{"contents":"사유","imageIds":[1,null]}""")

        assertThatThrownBy { request.toImageIds() }.isInstanceOf(InvalidAbsenceReasonImageException::class.java)
    }

    @Test
    fun `운영진 이미지 URL 조회는 사유서 목록과 같은 권한을 요구한다`() {
        // 값 클래스 인자 때문에 JVM 메서드 이름이 바뀌므로 접두어로 찾는다.
        fun authorityOf(prefix: String): String =
            AnnotatedElementUtils
                .findMergedAnnotation(
                    AttendanceQueryController::class.java.declaredMethods.single { it.name.startsWith(prefix) },
                    PreAuthorize::class.java,
                )!!
                .value

        assertThat(authorityOf("getAbsenceReasonImage")).isEqualTo("hasAuthority('update:attendance')")
        assertThat(authorityOf("getAbsenceReasonImage")).isEqualTo(authorityOf("getSessionAbsenceReasons"))
    }

    @Test
    fun `운영진 이미지 URL 조회는 URL 을 감싸 캐시 금지로 응답한다`() {
        val imageQueryService = mock(AbsenceReasonImageQueryService::class.java)
        val url = ImageUrlResponse("https://storage.test/read/1", Instant.parse("2026-10-04T00:05:00Z"))
        given(imageQueryService.getAbsenceReasonImage(SessionId(1), MemberId(2), ImageId(3))).willReturn(url)
        val controller =
            AttendanceQueryController(
                mock(AttendanceQueryService::class.java),
                mock(AbsenceReasonQueryService::class.java),
                imageQueryService,
            )

        val response = controller.getAbsenceReasonImage(SessionId(1), MemberId(2), ImageId(3))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.headers.getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store, private")
        assertThat(response.body!!.data).isSameAs(url)
    }
}

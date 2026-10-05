package core.application.attendance

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import core.application.attendance.application.exception.InvalidAbsenceReasonImageException
import core.application.attendance.presentation.request.AbsenceReportCreateRequest
import core.application.attendance.presentation.request.AbsenceReportUpdateRequest
import core.domain.image.vo.ImageId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

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
}

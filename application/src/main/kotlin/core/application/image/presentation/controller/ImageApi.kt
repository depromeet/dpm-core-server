package core.application.image.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.image.presentation.request.ImageUploadCreateRequest
import core.application.image.presentation.response.ImageUploadCreateResponse
import core.application.image.presentation.response.ImageUploadResponse
import core.application.image.presentation.response.ImageUrlResponse
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "Image", description = "이미지 업로드/조회 API (비공개 저장소 직접 업로드, 업로드한 본인만 조회)")
interface ImageApi {
    @Operation(
        summary = "이미지 업로드 URL 발급",
        description =
            "JPEG 또는 PNG(10MiB, 2,500만 픽셀 이하)의 형식과 바이트 수를 보내면 10분짜리 업로드 URL 을 줍니다. " +
                "프론트는 uploadUrl 로 파일 원본을 그대로 PUT 하고(Content-Type 헤더는 요청한 contentType 과 같게, " +
                "Content-Encoding 없이) complete 를 부릅니다. URL 을 가진 사람은 만료 전까지 이 업로드 객체에 쓸 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "발급 성공. data: uploadId, uploadUrl, expiresAt (Cache-Control: no-store)",
            ),
            ApiResponse(responseCode = "400", description = "본문 형식 오류, size 가 0 이하"),
            ApiResponse(responseCode = "401", description = "로그인 필요"),
            ApiResponse(responseCode = "413", description = "10MiB 초과"),
            ApiResponse(responseCode = "415", description = "JPEG/PNG 가 아닌 contentType"),
            ApiResponse(responseCode = "503", description = "이미지 저장소 사용 불가"),
        ],
    )
    fun createUpload(
        memberId: MemberId,
        request: ImageUploadCreateRequest,
    ): ResponseEntity<CustomResponse<ImageUploadCreateResponse>>

    @Operation(
        summary = "이미지 업로드 완료",
        description =
            "업로드된 파일을 서버가 검증(시그니처, 실제 디코드, 크기·형식이 발급 요청과 같은지)하고 확정 저장한 뒤 imageId 를 줍니다. " +
                "202 이면 Retry-After 초 뒤 같은 요청을 다시 보냅니다. 여러 번 불러도 같은 imageId(또는 같은 오류)를 돌려줍니다. " +
                "업로드 URL 이 만료된 뒤에는 검증을 새로 시작하지 않습니다(410).",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "완료. data: imageId, contentType, size"),
            ApiResponse(
                responseCode = "202",
                description = "검증 또는 확정 저장 중. 같은 요청을 다시 보냅니다",
                headers = [Header(name = "Retry-After", description = "다시 보낼 때까지 기다릴 초")],
            ),
            ApiResponse(responseCode = "400", description = "손상/해상도 초과, 형식 불일치, 크기 불일치, 빈 파일"),
            ApiResponse(responseCode = "401", description = "로그인 필요"),
            ApiResponse(responseCode = "404", description = "없거나 본인 업로드가 아님"),
            ApiResponse(
                responseCode = "409",
                description = "아직 PUT 하지 않음(IMAGE-409-01, 올린 뒤 재시도), 검증 후 파일 변경·저장 실패(새로 업로드)",
            ),
            ApiResponse(responseCode = "410", description = "업로드 URL 만료. 새로 업로드"),
            ApiResponse(responseCode = "413", description = "업로드된 파일이 10MiB 초과"),
            ApiResponse(responseCode = "415", description = "JPEG/PNG 가 아님"),
            ApiResponse(
                responseCode = "429",
                description = "다른 이미지를 검증 중(서버당 1건)",
                headers = [Header(name = "Retry-After", description = "다시 보낼 때까지 기다릴 초")],
            ),
            ApiResponse(responseCode = "503", description = "이미지 저장소 사용 불가. 잠시 후 같은 요청을 다시 보냅니다"),
        ],
    )
    fun completeUpload(
        memberId: MemberId,
        uploadId: String,
    ): ResponseEntity<CustomResponse<ImageUploadResponse>>

    @Operation(
        summary = "이미지 조회 URL 발급",
        description =
            "업로드한 본인에게 원본을 읽을 수 있는 1분짜리 URL 을 줍니다. <img src> 에 바로 쓸 수 있으며 만료 후에는 다시 요청합니다. " +
                "URL 을 가진 사람은 만료 전까지 누구나 읽을 수 있습니다. 남의 이미지와 없는 이미지는 모두 404 입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "data: url, expiresAt (Cache-Control: no-store)"),
            ApiResponse(responseCode = "401", description = "로그인 필요"),
            ApiResponse(responseCode = "404", description = "없거나 본인 이미지가 아님"),
            ApiResponse(responseCode = "503", description = "이미지 저장소 사용 불가"),
        ],
    )
    fun getImage(
        memberId: MemberId,
        imageId: ImageId,
    ): ResponseEntity<CustomResponse<ImageUrlResponse>>
}

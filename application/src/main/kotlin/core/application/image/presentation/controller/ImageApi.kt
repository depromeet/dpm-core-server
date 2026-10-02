package core.application.image.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.image.presentation.response.ImageUploadResponse
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Image", description = "이미지 업로드/조회 API (비공개 저장소, 업로드한 본인만 조회)")
interface ImageApi {
    @Operation(
        summary = "이미지 업로드",
        description =
            "multipart/form-data 의 file 파트로 JPEG 또는 PNG(10MiB, 2,500만 픽셀 이하)를 올립니다. " +
                "형식은 파일 내용으로 판별하며 파트의 Content-Type 이 있으면 내용과 같아야 합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "업로드 성공. data: imageId, contentType, size"),
            ApiResponse(responseCode = "400", description = "빈 파일, file 파트 누락, Content-Type 불일치, 손상/해상도 초과"),
            ApiResponse(responseCode = "401", description = "로그인 필요"),
            ApiResponse(responseCode = "413", description = "10MiB 초과"),
            ApiResponse(responseCode = "415", description = "JPEG/PNG 가 아닌 파일"),
            ApiResponse(responseCode = "503", description = "이미지 저장소 사용 불가"),
        ],
    )
    fun uploadImage(
        memberId: MemberId,
        file: MultipartFile,
    ): CustomResponse<ImageUploadResponse>

    @Operation(
        summary = "이미지 원본 조회",
        description =
            "업로드한 본인만 원본 바이트를 받습니다. 남의 이미지와 없는 이미지는 모두 404 입니다. " +
                "인증 헤더가 필요하므로 <img src> 대신 fetch 로 받아 Blob URL 로 표시합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "이미지 바이트",
                content = [
                    Content(mediaType = "image/jpeg", schema = Schema(type = "string", format = "binary")),
                    Content(mediaType = "image/png", schema = Schema(type = "string", format = "binary")),
                ],
            ),
            ApiResponse(responseCode = "401", description = "로그인 필요"),
            ApiResponse(responseCode = "404", description = "없거나 본인 이미지가 아님"),
            ApiResponse(responseCode = "503", description = "이미지 저장소 사용 불가"),
        ],
    )
    fun getImage(
        memberId: MemberId,
        imageId: ImageId,
    ): ResponseEntity<ByteArray>
}

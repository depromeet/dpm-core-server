package core.application.image.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.common.exception.GlobalExceptionCode
import core.application.image.application.dto.ImageUploadCompletion
import core.application.image.application.service.ImageCommandService
import core.application.image.application.service.ImageQueryService
import core.application.image.presentation.request.ImageUploadCreateRequest
import core.application.image.presentation.response.ImageUploadCreateResponse
import core.application.image.presentation.response.ImageUploadResponse
import core.application.image.presentation.response.ImageUrlResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

// SecurityConfig 가 /v3/** 를 permitAll 하므로 인증은 메서드에서 강제한다. 남의 이미지는 운영진만 본다.
// PAR URL 이 담긴 응답은 캐시되지 않게 no-store 를 붙인다.
@RestController
class ImageController(
    private val imageCommandService: ImageCommandService,
    private val imageQueryService: ImageQueryService,
) : ImageApi {
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/v3/images/uploads")
    override fun createUpload(
        @CurrentMemberId memberId: MemberId,
        @RequestBody request: ImageUploadCreateRequest,
    ): ResponseEntity<CustomResponse<ImageUploadCreateResponse>> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noStore())
            .body(
                CustomResponse.created(
                    imageCommandService.createUpload(memberId, request.contentType, request.size, request.fileName),
                ),
            )

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/v3/images/uploads/{uploadId}/complete")
    override fun completeUpload(
        @CurrentMemberId memberId: MemberId,
        @PathVariable uploadId: String,
    ): ResponseEntity<CustomResponse<ImageUploadResponse>> =
        when (val result = imageCommandService.completeUpload(memberId, uploadId)) {
            is ImageUploadCompletion.Completed -> ResponseEntity.ok(CustomResponse.ok(result.image))
            ImageUploadCompletion.InProgress ->
                ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                    .body(CustomResponse.ok<ImageUploadResponse>(null, GlobalExceptionCode.ACCEPTED))
        }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/v3/images/{imageId}")
    override fun getImage(
        @CurrentMemberId memberId: MemberId,
        @PathVariable imageId: ImageId,
    ): ResponseEntity<CustomResponse<ImageUrlResponse>> =
        ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(CustomResponse.ok(imageQueryService.getImage(memberId, imageId, canReadOthers())))

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/v3/images/{imageId}/download")
    override fun downloadImage(
        @CurrentMemberId memberId: MemberId,
        @PathVariable imageId: ImageId,
    ): ResponseEntity<Void> = redirectToDownload(imageQueryService.getDownloadUrl(memberId, imageId, canReadOthers()))

    // 운영진은 결석 사유서 증빙을 확인하므로 남의 이미지도 본다.
    private fun canReadOthers(): Boolean =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.authorities
            .orEmpty()
            .any { it.authority == READ_OTHERS_AUTHORITY }

    companion object {
        private const val READ_OTHERS_AUTHORITY = "update:attendance"
        private const val RETRY_AFTER_SECONDS = "1"
    }
}

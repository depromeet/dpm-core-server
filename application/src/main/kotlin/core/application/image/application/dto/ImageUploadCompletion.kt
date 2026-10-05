package core.application.image.application.dto

import core.application.image.presentation.response.ImageUploadResponse

sealed interface ImageUploadCompletion {
    data class Completed(
        val image: ImageUploadResponse,
    ) : ImageUploadCompletion

    /** 다른 요청이 처리 중이거나 복사가 끝나지 않았다. 같은 complete 를 다시 부른다. */
    data object InProgress : ImageUploadCompletion
}

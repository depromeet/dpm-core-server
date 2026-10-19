package core.application.attendance.presentation.response

data class AbsenceReasonImageInfo(
    val imageId: Long,
    /** 업로드할 때 받은 원본 파일명. 파일명 없이 올렸거나 기능 도입 전 이미지는 null */
    val fileName: String?,
)

package core.domain.image.enums

/**
 * 직접 업로드 세션 상태.
 *
 * PENDING → VERIFYING → COPYING → COMPLETED 순서로만 나아가며 COMPLETED, REJECTED, FAILED, EXPIRED 는 끝 상태다.
 * VERIFYING/COPYING 의 처리 주체는 lease(token + until)로 정하고, 상태 변경은 모두 token 이 일치할 때만 반영한다.
 */
enum class ImageUploadStatus {
    /** 업로드 URL 발급됨. 프론트가 PUT 한 뒤 complete 를 부른다. */
    PENDING,

    /** 서버가 업로드된 객체를 내려받아 검증 중. */
    VERIFYING,

    /** 검증 통과(ETag 고정). 확정 키로 복사 중. */
    COPYING,

    /** 이미지 행 생성 완료. uploadId → imageId 매핑으로 계속 남는다. */
    COMPLETED,

    /** 검증 실패. 같은 complete 호출에 같은 오류를 돌려준다. */
    REJECTED,

    /** 검증 후 원본이 바뀌었거나 복사가 실패했다. 새로 업로드해야 한다. */
    FAILED,

    /** 검증을 시작하지 못하고 버려졌다(정리 작업이 표시). */
    EXPIRED,
}

package core.entity.absencereason

import core.domain.absencereason.port.outbound.AbsenceReasonImagePersistencePort
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/** 결석 사유서 - 이미지 링크. UNIQUE(image_id) 로 이미지 하나는 한 사유서에만 붙는다. */
@Entity
@Table(
    name = "absence_reason_images",
    uniqueConstraints = [
        UniqueConstraint(
            name = AbsenceReasonImagePersistencePort.IMAGE_UNIQUE_CONSTRAINT,
            columnNames = ["image_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_absence_reason_images_reason_order", columnList = "absence_reason_id, display_order"),
    ],
)
class AbsenceReasonImageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "absence_reason_image_id", nullable = false, updatable = false)
    val id: Long = 0L,
    @Column(name = "absence_reason_id", nullable = false, updatable = false)
    val absenceReasonId: Long,
    @Column(name = "image_id", nullable = false, updatable = false)
    val imageId: Long,
    @Column(name = "display_order", nullable = false, updatable = false)
    val displayOrder: Int,
)

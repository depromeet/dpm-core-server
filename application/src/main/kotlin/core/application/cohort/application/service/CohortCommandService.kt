package core.application.cohort.application.service

import core.application.cohort.application.exception.CohortAlreadyExistsException
import core.application.cohort.application.exception.CohortReferencedException
import core.application.member.application.service.TrackMemberBadges
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
class CohortCommandService(
    private val cohortPersistencePort: CohortPersistencePort,
    private val cohortQueryService: CohortQueryService,
    private val cohortRoleService: CohortRoleService,
) {
    @TrackMemberBadges
    fun create(value: String): Cohort {
        val normalizedValue = value.trim()
        if (cohortPersistencePort.existsByValue(normalizedValue)) {
            throw CohortAlreadyExistsException()
        }

        val saved =
            cohortPersistencePort.save(
                Cohort(
                    value = normalizedValue,
                ),
            )
        if (cohortQueryService.isNumericCohortValue(normalizedValue)) {
            cohortRoleService.createLatestCohortRoles(normalizedValue)
        }
        return saved
    }

    @TrackMemberBadges
    fun update(
        cohortId: CohortId,
        value: String,
    ): Cohort {
        val existing = cohortQueryService.getCohort(cohortId)
        val normalizedValue = value.trim()
        if (existing.value != normalizedValue && cohortPersistencePort.existsByValue(normalizedValue)) {
            throw CohortAlreadyExistsException()
        }

        return cohortPersistencePort.save(
            Cohort(
                id = cohortId,
                value = normalizedValue,
                createdAt = existing.createdAt,
            ),
        )
    }

    @TrackMemberBadges
    fun delete(cohortId: CohortId) {
        cohortQueryService.getCohort(cohortId)
        if (cohortPersistencePort.hasAnyReference(cohortId)) {
            throw CohortReferencedException()
        }
        cohortPersistencePort.deleteById(cohortId)
    }

    @TrackMemberBadges
    fun activateCohort(cohortId: CohortId): Cohort {
        cohortQueryService.getCohort(cohortId)
        cohortPersistencePort.activate(cohortId)
        return cohortQueryService.getCohort(cohortId)
    }
}

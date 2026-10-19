package core.application.cohort.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CohortRoleService {
    fun createLatestCohortRoles(newCohortValue: String) {
        // Phase 1: canonical roles(CORE/ORGANIZER/DEEPER/GUEST) are fixed. Per-cohort role rows are no longer created.
    }
}

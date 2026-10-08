package core.persistence.sessionFeedback.repository

import core.entity.sessionFeedback.SessionFeedbackEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SessionFeedbackJpaRepository : JpaRepository<SessionFeedbackEntity, Long> {
    fun findBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): SessionFeedbackEntity?

    fun existsBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): Boolean

    @Query(
        "select distinct f from SessionFeedbackEntity f " +
            "left join fetch f.aspects " +
            "where f.sessionId = :sessionId",
    )
    fun findAllBySessionId(
        @Param("sessionId") sessionId: Long,
    ): List<SessionFeedbackEntity>

    fun countBySessionId(sessionId: Long): Long
}

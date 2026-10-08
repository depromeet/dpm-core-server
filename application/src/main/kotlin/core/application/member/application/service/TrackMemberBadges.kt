package core.application.member.application.service

import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

/** This write can change a management card. Reconcile once, at the end of its transaction. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Transactional(isolation = Isolation.READ_COMMITTED)
annotation class TrackMemberBadges

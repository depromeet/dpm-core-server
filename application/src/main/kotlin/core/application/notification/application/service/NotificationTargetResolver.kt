package core.application.notification.application.service

import core.domain.member.aggregate.InviteTagSpec
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Component

@Component
class NotificationTargetResolver(
    private val memberQueryUseCase: MemberQueryUseCase,
) {
    fun resolve(tags: List<InviteTagSpec>): List<MemberId> {
        val normalized = tags.distinctBy { Triple(it.cohortId, it.authorityId, it.tagName) }
        if (normalized.isEmpty()) {
            return emptyList()
        }

        return normalized
            .flatMap { tag ->
                memberQueryUseCase.findAllMemberIdsByCohortIdAndAuthorityId(
                    tag.cohortId,
                    tag.authorityId,
                )
            }.distinct()
    }
}

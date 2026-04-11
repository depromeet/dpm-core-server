package core.application.afterParty.application.exception

import core.application.common.exception.BusinessException
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId

class InviteTagNotFoundException(
    cohortId: CohortId,
    authorityId: AuthorityId,
) : BusinessException(
        AfterPartyExceptionCode.INVITE_TAG_NOT_FOUND,
    ) {
    override val message: String =
        "일치하는 태그를 찾을 수 없습니다: cohortId=${cohortId.value}, authorityId=${authorityId.value}"
}

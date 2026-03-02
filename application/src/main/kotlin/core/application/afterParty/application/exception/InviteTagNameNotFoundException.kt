package core.application.afterParty.application.exception

import core.application.common.exception.BusinessException

class InviteTagNameNotFoundException(
    tagName: String,
) : BusinessException(
        AfterPartyExceptionCode.INVITE_TAG_NAME_NOT_FOUND,
    ) {
    override val message: String = "초대 태그 이름과 매칭되는 태그를 찾을 수 없습니다: $tagName"
}

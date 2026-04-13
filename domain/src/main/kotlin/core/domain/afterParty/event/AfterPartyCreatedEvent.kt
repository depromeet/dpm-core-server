package core.domain.afterParty.event

import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import core.domain.notification.enums.NotificationMessageType

data class AfterPartyCreatedEvent(
    val afterPartyId: AfterPartyId,
    val title: String,
    val messageType: NotificationMessageType,
    val invitedMemberIds: List<MemberId>,
) {
    companion object {
        fun of(
            afterPartyId: AfterPartyId,
            title: String,
            messageType: NotificationMessageType = NotificationMessageType.AFTER_PARTY_INVITATION,
            invitedMemberIds: List<MemberId>,
        ): AfterPartyCreatedEvent =
            AfterPartyCreatedEvent(
                afterPartyId = afterPartyId,
                title = title,
                messageType = messageType,
                invitedMemberIds = invitedMemberIds,
            )
    }
}

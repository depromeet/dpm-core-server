package core.application.session.presentation.response

import java.time.LocalDateTime

data class SessionPolicyUpdateTargetResponse(
    val targeted: List<TargetedResponse>,
    val untargeted: List<UntargetedResponse>,
) {
    data class TargetedResponse(
        val name: String,
        val from: String,
        val to: String,
        val attendedAt: LocalDateTime?,
    )

    data class UntargetedResponse(
        val name: String,
        val status: String,
        val updatedAt: LocalDateTime?,
    )
}

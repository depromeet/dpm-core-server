package core.application.member.presentation.request

data class WhiteListCheckRequest(
    val name: String,
    val signupEmail: String,
)

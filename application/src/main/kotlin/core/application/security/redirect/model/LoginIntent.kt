package core.application.security.redirect.model

enum class LoginIntent {
    DIRECT, // 로그인 페이지로 들어온 경우
    REDIRECT, // 보호된 자원에 접근하기 위해 로그인 페이지로 리다이렉트된 경우
}

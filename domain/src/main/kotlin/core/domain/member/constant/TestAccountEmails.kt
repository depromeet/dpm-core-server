package core.domain.member.constant

/** 운영 목록(출석·멤버 관리)에서 숨길 QA 계정. 로그인·마이페이지는 그대로 둔다. */
object TestAccountEmails {
    const val SUFFIX = "@dpm.test"

    fun matches(email: String?): Boolean = email?.endsWith(SUFFIX, ignoreCase = true) == true
}

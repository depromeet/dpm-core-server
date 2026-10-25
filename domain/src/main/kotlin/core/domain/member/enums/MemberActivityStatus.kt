package core.domain.member.enums

/** 가입 상태와 독립적인 기수별 활동 상태. 종료와 정지는 같은 기수에서 되돌리지 않는다. */
enum class MemberActivityStatus { ACTIVE, SUSPENDED, ENDED }

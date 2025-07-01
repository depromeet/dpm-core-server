package com.server.dpmcore.member.member.domain

/**
 * 서비스 내 회원 상태를 나타냅니다.
 *
 * 이 열거형은 회원의 생애 주기(Lifecycle)에서 가질 수 있는 다양한 상태를 정의하며,
 * 각 상태는 회원의 활동 가능 여부를 나타냅니다.
 *
 * @property PENDING 회원이 생성되었지만 아직 활성화되지 않은 상태입니다. 운영진의 승인을 기다리는 상태를 의미합니다.
 * @property ACTIVE 회원이 정상적으로 활동 중인 상태입니다. 서비스의 주요 기능을 자유롭게 이용할 수 있습니다.
 * @property INACTIVE 이전 기수 수료자 또는 소속 회원으로, 일부 기능이 제한될 수 있습니다.
 * @property WITHDRAWN 회원이 자발적으로 중도 포기했거나, 수료 실패 등으로 탈퇴한 상태입니다. 더 이상 서비스에서 유효하지 않은 상태입니다.
 *
 * 추후 필요에 따라 새로운 상태가 추가될 수 있습니다.
 */
enum class MemberStatus {
    PENDING,
    ACTIVE,
    INACTIVE,
    WITHDRAWN,
    ;
}

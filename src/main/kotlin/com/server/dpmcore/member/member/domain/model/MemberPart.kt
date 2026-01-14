package com.server.dpmcore.member.member.domain.model

/**
 * 서비스 내 회원의 파트를 나타냅니다.
 *
 * 이 열거형은 회원이 속할 수 있는 파트(분야)를 정의하며, 각 파트는 회원의 역할과 전문 영역을 나타냅니다.
 * 한 회원은 동시에 여러 파트에 속할 수 없습니다.
 *
 *
 * @property WEB 웹 파트입니다.
 * @property SERVER 서버 파트입니다.
 * @property DESIGN 디자인 파트입니다.
 * @property IOS iOS 파트입니다.
 * @property ANDROID 안드로이드 파트입니다.
 *
 * 추후 필요에 따라 새로운 파트가 추가되거나 삭제될 수 있습니다.
 */
enum class MemberPart {
    WEB,
    SERVER,
    DESIGN,
    IOS,
    ANDROID,
}

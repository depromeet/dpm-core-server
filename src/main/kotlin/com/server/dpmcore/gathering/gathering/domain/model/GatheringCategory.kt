package com.server.dpmcore.gathering.gathering.domain.model

/**
 * 1차 MVP에서는 '디프만 회식'만 존재하지만, 이후 생길 수 있는 회식의 종류에 대응하기 위한 클래스입니다.
 */
enum class GatheringCategory(val value: String) {
    GATHERING("gathering"),
}

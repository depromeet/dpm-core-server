package com.server.dpmcore.common.util

const val PAGE_SIZE = 20 // 필요한 위치에 정의

data class PaginatedResult<T>(
    val content: List<T>,
    val hasNext: Boolean,
    val nextCursorId: Long?,
)

inline fun <T> List<T>.paginate(
    pageSize: Int = PAGE_SIZE,
    crossinline idSelector: (T) -> Long,
): PaginatedResult<T> {
    val sorted = this.sortedBy { idSelector(it) }

    return if (sorted.size > pageSize) {
        PaginatedResult(
            content = sorted.take(pageSize),
            hasNext = true,
            nextCursorId = idSelector(sorted.last()),
        )
    } else {
        PaginatedResult(
            content = sorted,
            hasNext = false,
            nextCursorId = null,
        )
    }
}

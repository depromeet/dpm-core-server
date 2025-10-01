package core.application.common.extension

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
    val content = if (this.size > pageSize) this.take(pageSize) else this

    return if (this.size > pageSize) {
        PaginatedResult(
            content = content,
            hasNext = true,
            nextCursorId = idSelector(content.last()),
        )
    } else {
        PaginatedResult(
            content = content,
            hasNext = false,
            nextCursorId = null,
        )
    }
}

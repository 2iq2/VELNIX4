package com.velnix.tv.domain.model

/** Electronic programme guide item. Times are UTC epoch milliseconds. */
data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startUtcMillis: Long,
    val endUtcMillis: Long
) {
    init {
        require(channelId.isNotBlank())
        require(title.isNotBlank())
        require(endUtcMillis > startUtcMillis)
    }

    fun progressAt(nowUtcMillis: Long): Float {
        if (nowUtcMillis <= startUtcMillis) return 0f
        if (nowUtcMillis >= endUtcMillis) return 1f
        return (nowUtcMillis - startUtcMillis).toFloat() /
            (endUtcMillis - startUtcMillis).toFloat()
    }
}

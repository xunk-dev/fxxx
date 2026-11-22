package com.frameout.engine.repair

class DropRanges {
    private val spans = ArrayList<LongArray>()

    val isEmpty: Boolean get() = spans.isEmpty()
    val count: Int get() = spans.size

    val totalUs: Long
        get() = spans.sumOf { it[1] - it[0] }

    fun add(startUs: Long, endUs: Long) {
        if (endUs <= startUs) return
        spans.add(longArrayOf(startUs.coerceAtLeast(0L), endUs))
        merge()
    }

    fun contains(us: Long): Boolean = spans.any { us >= it[0] && us < it[1] }

    fun shiftAt(us: Long): Long = spans.sumOf { span ->
        when {
            us >= span[1] -> span[1] - span[0]
            us > span[0] -> us - span[0]
            else -> 0L
        }
    }

    fun copy(): DropRanges = DropRanges().also { clone -> spans.forEach { clone.spans.add(it.copyOf()) } }

    fun toList(): List<LongRange> = spans.map { it[0]..it[1] }

    private fun merge() {
        spans.sortBy { it[0] }
        var i = 0
        while (i < spans.size - 1) {
            val current = spans[i]
            val next = spans[i + 1]
            if (next[0] <= current[1]) {
                current[1] = maxOf(current[1], next[1])
                spans.removeAt(i + 1)
            } else {
                i++
            }
        }
    }
}

package com.frameout.core.collage

const val HISTORY_LIMIT = 60

data class HistoryStack<T>(val undo: List<T> = emptyList(), val redo: List<T> = emptyList(), val limit: Int = HISTORY_LIMIT) {
    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()

    fun commit(previous: T): HistoryStack<T> = copy(undo = (undo + previous).takeLast(limit), redo = emptyList())

    fun undone(current: T): Pair<T, HistoryStack<T>>? {
        val target = undo.lastOrNull() ?: return null
        return target to copy(undo = undo.dropLast(1), redo = (redo + current).takeLast(limit))
    }

    fun redone(current: T): Pair<T, HistoryStack<T>>? {
        val target = redo.lastOrNull() ?: return null
        return target to copy(undo = (undo + current).takeLast(limit), redo = redo.dropLast(1))
    }
}

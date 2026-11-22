package com.frameout.core.edit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UnsavedEdits {
    private val stamps = LinkedHashMap<Long, Long>()
    private val acknowledged = HashMap<Long, Long>()
    private var clock = 0L
    private val _version = MutableStateFlow(0)

    val version: StateFlow<Int> = _version.asStateFlow()

    fun mark(frameId: Long, unsaved: Boolean) = markAll(listOf(frameId), unsaved)

    fun markAll(frameIds: Collection<Long>, unsaved: Boolean) {
        if (frameIds.isEmpty()) return
        val changed = synchronized(stamps) {
            if (unsaved) {
                var changed = false
                frameIds.forEach { id ->
                    val old = stamps[id]
                    if (old == null || acknowledged[id] == old) changed = true
                    stamps[id] = ++clock
                }
                changed
            } else {
                var removed = false
                frameIds.forEach { id ->
                    if (stamps.remove(id) != null) removed = true
                    acknowledged.remove(id)
                }
                removed
            }
        }
        if (changed) bump()
    }

    fun clean(frameId: Long) = mark(frameId, false)

    fun isUnsaved(frameId: Long): Boolean = synchronized(stamps) { frameId in stamps }

    fun snapshot(): List<Long> = synchronized(stamps) { stamps.keys.toList() }

    fun now(): Long = synchronized(stamps) { clock }

    fun pending(since: Long = 0L): List<Long> = synchronized(stamps) {
        stamps.filter { (id, stamp) -> stamp > since && stamp > (acknowledged[id] ?: 0L) }.keys.toList()
    }

    fun acknowledge(frameIds: Collection<Long>) {
        if (frameIds.isEmpty()) return
        val changed = synchronized(stamps) {
            var any = false
            frameIds.forEach { id ->
                val stamp = stamps[id] ?: return@forEach
                if (acknowledged[id] != stamp) {
                    acknowledged[id] = stamp
                    any = true
                }
            }
            any
        }
        if (changed) bump()
    }

    fun clear() {
        val changed = synchronized(stamps) {
            acknowledged.clear()
            stamps.isNotEmpty().also { stamps.clear() }
        }
        if (changed) bump()
    }

    private fun bump() {
        _version.value = _version.value + 1
    }
}

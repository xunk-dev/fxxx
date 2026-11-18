package com.frameout.core.edit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UnsavedEdits {
    private val ids = LinkedHashSet<Long>()
    private val _version = MutableStateFlow(0)

    val version: StateFlow<Int> = _version.asStateFlow()

    fun mark(frameId: Long, unsaved: Boolean) = markAll(listOf(frameId), unsaved)

    fun markAll(frameIds: Collection<Long>, unsaved: Boolean) {
        if (frameIds.isEmpty()) return
        val changed = synchronized(ids) { if (unsaved) ids.addAll(frameIds) else ids.removeAll(frameIds.toSet()) }
        if (changed) _version.value = _version.value + 1
    }

    fun clean(frameId: Long) = mark(frameId, false)

    fun isUnsaved(frameId: Long): Boolean = synchronized(ids) { frameId in ids }

    fun snapshot(): List<Long> = synchronized(ids) { ids.toList() }

    fun clear() {
        val changed = synchronized(ids) { ids.isNotEmpty().also { ids.clear() } }
        if (changed) _version.value = _version.value + 1
    }
}

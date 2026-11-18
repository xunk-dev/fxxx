package com.frameout.core.quality

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

object ScanThrottle {
    val scrolling = MutableStateFlow(false)

    val interactive = MutableStateFlow(0)

    suspend fun awaitIdle() {
        if (scrolling.value || interactive.value > 0) {
            combine(scrolling, interactive) { s, i -> s || i > 0 }.first { !it }
        }
    }

    suspend fun <T> runInteractive(block: suspend () -> T): T {
        interactive.update { it + 1 }
        try {
            return block()
        } finally {
            interactive.update { it - 1 }
        }
    }
}

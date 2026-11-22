package com.frameout.core.util

import kotlinx.coroutines.CancellationException

suspend fun <T> readOrKeep(previous: T, read: suspend () -> T): T =
    try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        previous
    }

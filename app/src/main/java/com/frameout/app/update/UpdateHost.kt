package com.frameout.app.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private const val SPLASH_GRACE_MS = 2200L

@Composable
fun UpdateHost(controller: UpdateController, blocked: Boolean) {
    val info = controller.info ?: return
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SPLASH_GRACE_MS)
        armed = true
    }
    UpdateSheet(
        visible = controller.visible && armed && !blocked,
        info = info,
        onLater = controller::later,
        onUpdate = controller::updateNow
    )
}

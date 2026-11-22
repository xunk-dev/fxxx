package com.frameout.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

@Composable
internal fun windowHeightDp(): Dp {
    val density = LocalDensity.current
    val height = LocalWindowInfo.current.containerSize.height
    return with(density) { height.toDp() }
}

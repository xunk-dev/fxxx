package com.frameout.app.viewer

import androidx.compose.ui.geometry.Offset

private const val PEEK_SCALE = 3f
private const val PEEK_RESET_FROM = 1.05f

internal fun ViewerViewModel.peekZoom(scale: Float, press: Offset, center: Offset, area: Offset) {
    if (scale > PEEK_RESET_FROM) {
        setTransform(1f, 0f, 0f)
        return
    }
    val limitX = area.x * (PEEK_SCALE - 1f) / 2f
    val limitY = area.y * (PEEK_SCALE - 1f) / 2f
    setTransform(
        PEEK_SCALE,
        ((center.x - press.x) * (PEEK_SCALE - 1f)).coerceIn(-limitX, limitX),
        ((center.y - press.y) * (PEEK_SCALE - 1f)).coerceIn(-limitY, limitY)
    )
}

package com.frameout.app.viewer

internal class DetectionRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

internal fun DetectionFit.rectOf(detection: Detection): DetectionRect {
    val left = x(detection.box.l)
    val top = y(detection.box.t)
    val width = (x(detection.box.r) - left).coerceAtLeast(1f)
    val height = (y(detection.box.b) - top).coerceAtLeast(1f)
    return DetectionRect(left, top, width, height)
}

internal fun Detection.area(): Float = box.width * box.height

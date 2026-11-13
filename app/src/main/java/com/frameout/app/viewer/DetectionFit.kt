package com.frameout.app.viewer

internal class DetectionFit(
    width: Float,
    height: Float,
    imageWidth: Int,
    imageHeight: Int,
    private val scale: Float,
    private val offsetX: Float,
    private val offsetY: Float
) {
    private val fit = minOf(width / imageWidth, height / imageHeight)
    private val displayW = imageWidth * fit
    private val displayH = imageHeight * fit
    private val originX = (width - displayW) / 2f
    private val originY = (height - displayH) / 2f
    private val centerX = width / 2f
    private val centerY = height / 2f

    fun x(u: Float): Float = centerX + (originX + u * displayW - centerX) * scale + offsetX

    fun y(v: Float): Float = centerY + (originY + v * displayH - centerY) * scale + offsetY
}

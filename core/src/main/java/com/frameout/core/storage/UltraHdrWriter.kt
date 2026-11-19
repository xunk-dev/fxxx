package com.frameout.core.storage

import android.graphics.Bitmap
import com.frameout.core.hdr.LegacyGain
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Locale
import kotlin.math.ln

internal object UltraHdrWriter {
    private const val GAIN_QUALITY = 95
    private const val XMP_ID = "http://ns.adobe.com/xap/1.0/\u0000"
    private val LN2 = ln(2.0)

    fun write(bitmap: Bitmap, gain: LegacyGain, out: OutputStream): Boolean {
        val primary = encodePrimary(bitmap) ?: return false
        val gainJpeg = encodeGain(gain) ?: return false
        out.write(assemble(primary, gainJpeg, gainXmp(gain.ratioMin, gain.ratioMax, gain.epsilon, gain.fullHdrRatio)))
        out.flush()
        return true
    }

    internal fun assemble(primary: ByteArray, gainJpeg: ByteArray, gainXmp: String): ByteArray {
        val gainFile = JpegSegments.insert(gainJpeg, listOf(app1(gainXmp)))
        return assembleWith(primary, gainFile)
    }

    private fun assembleWith(primary: ByteArray, gainFile: ByteArray): ByteArray {
        val xmp = app1(primaryXmp(gainFile.size))
        val mpfLength = MPF_SEGMENT_LENGTH
        val headerEnd = JpegSegments.headerEnd(primary)
        val total = primary.size + xmp.size + mpfLength
        val mpfMarkerAt = headerEnd + xmp.size
        val mpfBase = mpfMarkerAt + 8
        val mpf = mpfSegment(total, gainFile.size, total - mpfBase)
        val result = ByteArrayOutputStream(total + gainFile.size)
        result.write(primary, 0, headerEnd)
        result.write(xmp)
        result.write(mpf)
        result.write(primary, headerEnd, primary.size - headerEnd)
        result.write(gainFile)
        return result.toByteArray()
    }

    private fun encodePrimary(bitmap: Bitmap): ByteArray? {
        val buffer = ByteArrayOutputStream(bitmap.byteCount.coerceIn(1 shl 16, 1 shl 26) / 4)
        val ok = if (Jpeg444.supports(bitmap)) Jpeg444.encode(bitmap, 100, buffer) else bitmap.compress(Bitmap.CompressFormat.JPEG, 100, buffer)
        return if (ok) buffer.toByteArray() else null
    }

    private fun encodeGain(gain: LegacyGain): ByteArray? {
        if (gain.map.isRecycled) return null
        val buffer = ByteArrayOutputStream()
        return if (gain.map.compress(Bitmap.CompressFormat.JPEG, GAIN_QUALITY, buffer)) buffer.toByteArray() else null
    }

    internal fun app1(xmp: String): ByteArray {
        val payload = (XMP_ID + xmp).toByteArray(Charsets.UTF_8)
        return segment(0xE1, payload)
    }

    private fun segment(marker: Int, payload: ByteArray): ByteArray {
        require(payload.size + 2 <= 0xFFFF) { "segment too large" }
        val out = ByteArray(payload.size + 4)
        out[0] = 0xFF.toByte()
        out[1] = marker.toByte()
        out[2] = ((payload.size + 2) shr 8).toByte()
        out[3] = (payload.size + 2).toByte()
        System.arraycopy(payload, 0, out, 4, payload.size)
        return out
    }

    internal fun primaryXmp(gainLength: Int): String = """<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.2">
  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <rdf:Description xmlns:Container="http://ns.google.com/photos/1.0/container/" xmlns:Item="http://ns.google.com/photos/1.0/container/item/" xmlns:hdrgm="http://ns.adobe.com/hdr-gain-map/1.0/" hdrgm:Version="1.0">
      <Container:Directory>
        <rdf:Seq>
          <rdf:li rdf:parseType="Resource">
            <Container:Item Item:Semantic="Primary" Item:Mime="image/jpeg"/>
          </rdf:li>
          <rdf:li rdf:parseType="Resource">
            <Container:Item Item:Semantic="GainMap" Item:Mime="image/jpeg" Item:Length="$gainLength"/>
          </rdf:li>
        </rdf:Seq>
      </Container:Directory>
    </rdf:Description>
  </rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>"""

    internal fun gainXmp(ratioMin: FloatArray, ratioMax: FloatArray, epsilon: Float, fullHdrRatio: Float): String {
        val min = DoubleArray(3) { log2(ratioMin[it]) }
        val max = DoubleArray(3) { log2(ratioMax[it]) }
        val eps = epsilon.toDouble()
        val capacity = log2(fullHdrRatio.coerceAtLeast(1f))
        val single = min.all { it == min[0] } && max.all { it == max[0] }
        val scalars = StringBuilder()
        val children = StringBuilder()
        if (single) {
            scalars.append(" hdrgm:GainMapMin=\"${num(min[0])}\" hdrgm:GainMapMax=\"${num(max[0])}\"")
        } else {
            children.append(seq("GainMapMin", min)).append(seq("GainMapMax", max))
        }
        scalars.append(" hdrgm:Gamma=\"1\" hdrgm:OffsetSDR=\"${num(eps)}\" hdrgm:OffsetHDR=\"${num(eps)}\"")
        scalars.append(" hdrgm:HDRCapacityMin=\"0\" hdrgm:HDRCapacityMax=\"${num(capacity)}\" hdrgm:BaseRenditionIsHDR=\"False\"")
        return """<?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.2">
  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <rdf:Description xmlns:hdrgm="http://ns.adobe.com/hdr-gain-map/1.0/" hdrgm:Version="1.0"$scalars>$children
    </rdf:Description>
  </rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>"""
    }

    private fun seq(name: String, values: DoubleArray): String =
        "\n      <hdrgm:$name><rdf:Seq>" + values.joinToString("") { "<rdf:li>${num(it)}</rdf:li>" } + "</rdf:Seq></hdrgm:$name>"

    private fun log2(ratio: Float): Double = ln(ratio.coerceAtLeast(1e-6f).toDouble()) / LN2

    private fun num(v: Double): String = String.format(Locale.ROOT, "%.6f", v).trimEnd('0').trimEnd('.').ifEmpty { "0" }

    private const val MPF_SEGMENT_LENGTH = 2 + 2 + 4 + 82

    internal fun mpfSegment(primarySize: Int, gainSize: Int, gainOffset: Int): ByteArray {
        val tiff = java.nio.ByteBuffer.allocate(82).order(java.nio.ByteOrder.BIG_ENDIAN)
        tiff.put('M'.code.toByte()).put('M'.code.toByte()).putShort(0x002A).putInt(8)
        tiff.putShort(3)
        tiff.putShort(0xB000.toShort()).putShort(7).putInt(4).put("0100".toByteArray(Charsets.US_ASCII))
        tiff.putShort(0xB001.toShort()).putShort(4).putInt(1).putInt(2)
        tiff.putShort(0xB002.toShort()).putShort(7).putInt(32).putInt(50)
        tiff.putInt(0)
        tiff.putInt(0x20030000).putInt(primarySize).putInt(0).putShort(0).putShort(0)
        tiff.putInt(0).putInt(gainSize).putInt(gainOffset).putShort(0).putShort(0)
        val payload = "MPF\u0000".toByteArray(Charsets.US_ASCII) + tiff.array()
        return segment(0xE2, payload)
    }
}

internal object JpegSegments {
    fun headerEnd(jpeg: ByteArray): Int {
        require(jpeg.size > 4 && jpeg[0] == 0xFF.toByte() && jpeg[1] == 0xD8.toByte()) { "not a JPEG" }
        var at = 2
        if (jpeg[at] == 0xFF.toByte() && jpeg[at + 1] == 0xE0.toByte()) {
            at += 2 + (((jpeg[at + 2].toInt() and 0xFF) shl 8) or (jpeg[at + 3].toInt() and 0xFF))
        }
        return at
    }

    fun insert(jpeg: ByteArray, segments: List<ByteArray>): ByteArray {
        val at = headerEnd(jpeg)
        val out = ByteArrayOutputStream(jpeg.size + segments.sumOf { it.size })
        out.write(jpeg, 0, at)
        segments.forEach { out.write(it) }
        out.write(jpeg, at, jpeg.size - at)
        return out.toByteArray()
    }
}

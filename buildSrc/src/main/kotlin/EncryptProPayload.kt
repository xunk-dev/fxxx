import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class EncryptProPayload : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val source: RegularFileProperty

    @get:Input
    abstract val certSha256: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val payload = encode(source.get().asFile.readLines())
        val cert = certSha256.get()
        val blob = if (cert.isEmpty()) MAGIC_PLAIN + payload else seal(payload, fingerprintOf(cert))
        val dir = outputDir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()
        dir.resolve(ASSET).writeBytes(blob)
    }

    private fun fingerprintOf(cert: String): ByteArray {
        if (cert.length != 64 || cert.any { it !in "0123456789abcdef" }) {
            throw GradleException("CERT_SHA256 must be 64 hex characters to seal the Pro payload.")
        }
        return ByteArray(32) { cert.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private fun encode(lines: List<String>): ByteArray {
        val rows = lines.map { it.trim() }.filter { it.isNotEmpty() }.map { it.split(Regex("\\s+")) }
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeShort(rows.size)
            for (row in rows) {
                val id = row.first().toByteArray(Charsets.UTF_8)
                if (id.isEmpty() || id.size > 255 || row.size != MATRIX_SIZE + 1) {
                    throw GradleException("Malformed Pro payload row: ${row.first()}")
                }
                out.writeByte(id.size)
                out.write(id)
                row.drop(1).forEach { out.writeFloat(it.toFloat()) }
            }
        }
        return bytes.toByteArray()
    }

    private fun seal(payload: ByteArray, fingerprint: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(fingerprint, "HmacSHA256"))
        mac.update(INFO.toByteArray(Charsets.US_ASCII))
        mac.update(1.toByte())
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(mac.doFinal(), "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC_SEALED)
        return MAGIC_SEALED + iv + cipher.doFinal(payload)
    }

    private companion object {
        const val ASSET = "pro_payload.bin"
        const val INFO = "frameout.pro.payload.v1"
        const val MATRIX_SIZE = 20
        val MAGIC_SEALED = byteArrayOf(0x46, 0x58, 0x50, 0x45)
        val MAGIC_PLAIN = byteArrayOf(0x46, 0x58, 0x50, 0x50)
    }
}

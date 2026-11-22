package com.frameout.core.pro

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object ProPrefs {
    private const val FILE = "frameout_pro_sealed"
    private const val ALIAS = "frameout_pro_key"
    private const val PROVIDER = "AndroidKeyStore"

    fun encrypted(context: Context): SharedPreferences {
        val store = SealedPrefs(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE), secretKey())
        store.verify()
        return store
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}

internal class SealedPrefs(private val delegate: SharedPreferences, private val key: SecretKey) : SharedPreferences {

    fun verify() {
        val anchor = slot(ANCHOR)
        val existing = delegate.getString(anchor, null)
        if (existing == null) {
            delegate.edit().putString(anchor, seal(anchor, encode(true))).commit()
        } else {
            check(open(anchor, existing) != null)
        }
    }

    override fun getAll(): MutableMap<String, *> = mutableMapOf<String, Any?>()

    override fun getString(key: String, defValue: String?): String? = read(key) as? String ?: defValue

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        (read(key) as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues

    override fun getInt(key: String, defValue: Int): Int = read(key) as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long = read(key) as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float = read(key) as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean = read(key) as? Boolean ?: defValue

    override fun contains(key: String): Boolean = read(key) != null

    override fun edit(): SharedPreferences.Editor = SealedEditor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

    private fun read(name: String): Any? {
        val s = slot(name)
        val text = delegate.getString(s, null) ?: return null
        val plain = open(s, text) ?: return null
        return decode(plain)
    }

    private fun slot(name: String): String =
        MessageDigest.getInstance("SHA-256").digest(name.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun seal(slot: String, plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(slot.toByteArray(Charsets.UTF_8))
        val body = cipher.doFinal(plain)
        return Base64.encodeToString(cipher.iv + body, Base64.NO_WRAP)
    }

    private fun open(slot: String, text: String): ByteArray? = runCatching {
        val raw = Base64.decode(text, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, raw, 0, IV_BYTES))
        cipher.updateAAD(slot.toByteArray(Charsets.UTF_8))
        cipher.doFinal(raw, IV_BYTES, raw.size - IV_BYTES)
    }.getOrNull()

    private fun encode(value: Any): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { d ->
            when (value) {
                is Boolean -> { d.writeByte(T_BOOL); d.writeBoolean(value) }
                is Int -> { d.writeByte(T_INT); d.writeInt(value) }
                is Long -> { d.writeByte(T_LONG); d.writeLong(value) }
                is Float -> { d.writeByte(T_FLOAT); d.writeFloat(value) }
                is String -> { d.writeByte(T_STRING); d.writeUTF(value) }
                is Set<*> -> {
                    val items = value.filterIsInstance<String>()
                    d.writeByte(T_SET)
                    d.writeInt(items.size)
                    items.forEach { d.writeUTF(it) }
                }
            }
        }
        return out.toByteArray()
    }

    private fun decode(bytes: ByteArray): Any? = runCatching {
        DataInputStream(ByteArrayInputStream(bytes)).use { d ->
            when (d.readUnsignedByte()) {
                T_BOOL -> d.readBoolean()
                T_INT -> d.readInt()
                T_LONG -> d.readLong()
                T_FLOAT -> d.readFloat()
                T_STRING -> d.readUTF()
                T_SET -> {
                    val size = d.readInt()
                    val items = LinkedHashSet<String>()
                    repeat(size) { items.add(d.readUTF()) }
                    items
                }
                else -> null
            }
        }
    }.getOrNull()

    private inner class SealedEditor : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, Any?>()
        private var wipe = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = stage(key, value)

        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = stage(key, values?.toSet())

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = stage(key, value)

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = stage(key, value)

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = stage(key, value)

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = stage(key, value)

        override fun remove(key: String): SharedPreferences.Editor = stage(key, null)

        override fun clear(): SharedPreferences.Editor {
            synchronized(pending) {
                wipe = true
                pending.clear()
            }
            return this
        }

        override fun commit(): Boolean = flush().commit()

        override fun apply() {
            flush().apply()
        }

        private fun stage(name: String, value: Any?): SharedPreferences.Editor {
            synchronized(pending) { pending[name] = value }
            return this
        }

        private fun flush(): SharedPreferences.Editor {
            val target = delegate.edit()
            synchronized(pending) {
                if (wipe) target.clear()
                pending.forEach { (name, value) ->
                    val s = slot(name)
                    if (value == null) target.remove(s) else target.putString(s, seal(s, encode(value)))
                }
                pending.clear()
                wipe = false
            }
            return target
        }
    }

    private companion object {
        const val ANCHOR = "__anchor"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val IV_BYTES = 12
        const val T_BOOL = 0
        const val T_INT = 1
        const val T_LONG = 2
        const val T_FLOAT = 3
        const val T_STRING = 4
        const val T_SET = 5
    }
}

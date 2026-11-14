package com.frameout.core.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccentSealTest {
    private class MemStore(var saved: String? = null) : GrantStore {
        override fun read(): String? = saved
        override fun write(value: String): Boolean {
            saved = value
            return true
        }
    }

    @Test
    fun matchingAccentNeedsNoRestore() {
        val seal = AccentSeal(MemStore("gold"), true)
        assertNull(seal.restoreTarget("gold"))
    }

    @Test
    fun editedAccentIsRestoredToTheSealedOne() {
        val seal = AccentSeal(MemStore("black"), true)
        assertEquals("black", seal.restoreTarget("gold"))
    }

    @Test
    fun adoptSealsOnlyOnce() {
        val seal = AccentSeal(MemStore(), true)
        seal.adopt("indigo")
        seal.adopt("gold")
        assertEquals("indigo", seal.key())
    }

    @Test
    fun invalidSealedValueIsIgnored() {
        val seal = AccentSeal(MemStore("hacked"), true)
        assertNull(seal.key())
        assertNull(seal.restoreTarget("gold"))
    }

    @Test
    fun inactiveSealNeverWritesOrRestores() {
        val store = MemStore()
        val seal = AccentSeal(store, false)
        seal.set("gold")
        assertNull(store.saved)
        assertNull(seal.restoreTarget("gold"))
    }

    @Test
    fun dynamicCanBeSealedAfterAPaidApply() {
        val seal = AccentSeal(MemStore(), true)
        seal.set("dynamic")
        assertEquals("dynamic", seal.key())
    }
}

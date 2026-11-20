package com.frameout.engine

import com.frameout.engine.repair.DropRanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DropRangesTest {
    @Test fun overlappingSpansMerge() {
        val drops = DropRanges()
        drops.add(1_000, 2_000)
        drops.add(1_500, 3_000)
        drops.add(5_000, 6_000)
        assertEquals(2, drops.count)
        assertEquals(3_000L, drops.totalUs)
    }

    @Test fun spanEndIsExclusive() {
        val drops = DropRanges().apply { add(100, 200) }
        assertTrue(drops.contains(100))
        assertTrue(drops.contains(199))
        assertFalse(drops.contains(200))
        assertFalse(drops.contains(99))
    }

    @Test fun shiftClosesTheGap() {
        val drops = DropRanges().apply { add(1_000, 2_000); add(5_000, 6_000) }
        assertEquals(0L, drops.shiftAt(500))
        assertEquals(1_000L, drops.shiftAt(2_000))
        assertEquals(1_000L, drops.shiftAt(4_000))
        assertEquals(2_000L, drops.shiftAt(9_000))
    }

    @Test fun emptyOrReversedSpansAreIgnored() {
        val drops = DropRanges().apply { add(500, 500); add(900, 100) }
        assertTrue(drops.isEmpty)
    }

    @Test fun copyIsIndependent() {
        val original = DropRanges().apply { add(0, 10) }
        val clone = original.copy()
        original.add(20, 30)
        assertEquals(1, clone.count)
    }
}

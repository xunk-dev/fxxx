package com.frameout.app.screens

import androidx.compose.ui.unit.dp
import com.frameout.designsystem.theme.FrameAdaptive
import com.frameout.designsystem.theme.FrameRail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AdaptiveLayoutTest(private val shape: ScreenShape) {
    private val adaptive = FrameAdaptive(shape.widthDp.dp, shape.heightDp.dp, false)

    @Test
    fun railFollowsWidthClass() {
        val expected = when {
            shape.widthDp >= 840 -> FrameRail.Expanded
            shape.widthDp >= 600 -> FrameRail.Compact
            else -> FrameRail.None
        }
        assertEquals(shape.toString(), expected, adaptive.rail)
    }

    @Test
    fun contentKeepsRoomBesideRail() {
        assertTrue("$shape usable ${adaptive.usableWidth}", adaptive.usableWidth >= 320.dp)
    }

    @Test
    fun railNeverTakesHalfTheWindow() {
        assertTrue(shape.toString(), adaptive.railInset.value * 2 <= adaptive.width.value)
    }

    @Test
    fun gridColumnsStayInRange() {
        for (base in 2..5) {
            val columns = adaptive.columns(base)
            assertTrue("$shape base $base gave $columns", columns in base..16)
        }
    }

    @Test
    fun gridCellsStayReadable() {
        val cell = adaptive.usableWidth.value / adaptive.columns(3)
        assertTrue("$shape cell $cell", cell >= 80f)
    }

    @Test
    fun listColumnsMatchUsableWidth() {
        val expected = when {
            adaptive.usableWidth >= 1100.dp -> 3
            adaptive.usableWidth >= 700.dp -> 2
            else -> 1
        }
        assertEquals(shape.toString(), expected, adaptive.listColumns())
    }

    @Test
    fun sideEditorOnlyWhenLandscape() {
        if (adaptive.sideEditor) assertTrue(shape.toString(), adaptive.landscape)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun shapes(): List<ScreenShape> = ScreenShapes.all
    }
}

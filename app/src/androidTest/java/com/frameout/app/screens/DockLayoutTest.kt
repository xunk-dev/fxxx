package com.frameout.app.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.dock.FrameDock
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DockLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    private val label = "Library"

    private fun show() {
        rule.setContent {
            FrameTheme {
                FrameDock(
                    items = listOf(
                        DockItem("library", label, FrameIcons.Library, selected = true, onClick = {}),
                        DockItem("settings", "Settings", FrameIcons.Settings, onClick = {})
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun dockRendersOnThisScreen() {
        show()
        val byDescription = rule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().size
        val byText = rule.onAllNodesWithText(label).fetchSemanticsNodes().size
        assertTrue("dock entry missing", byDescription + byText > 0)
    }

    @Test
    fun dockStaysInsideTheWindow() {
        show()
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(root.width > 0f && root.height > 0f)
        val nodes = rule.onAllNodesWithContentDescription(label).fetchSemanticsNodes() +
            rule.onAllNodesWithText(label).fetchSemanticsNodes()
        nodes.forEach { node ->
            val box = node.boundsInRoot
            assertTrue("entry left of window", box.left >= root.left - 1f)
            assertTrue("entry above window", box.top >= root.top - 1f)
            assertTrue("entry right of window", box.right <= root.right + 1f)
            assertTrue("entry below window", box.bottom <= root.bottom + 1f)
        }
    }
}

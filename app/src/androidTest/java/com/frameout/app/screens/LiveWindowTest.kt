package com.frameout.app.screens

import android.content.pm.ActivityInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.app.MainActivity
import com.frameout.designsystem.theme.FrameAdaptive
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveWindowTest {
    private val orientations = listOf(
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    )

    @Test
    fun launchesInEveryOrientation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            orientations.forEach { requested ->
                scenario.onActivity { it.requestedOrientation = requested }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                Thread.sleep(SETTLE_MS)
                assertTrue("not started after $requested", scenario.state.isAtLeast(Lifecycle.State.STARTED))
                scenario.onActivity { activity ->
                    val config = activity.resources.configuration
                    val adaptive = FrameAdaptive(config.screenWidthDp.dp, config.screenHeightDp.dp, false)
                    assertTrue("window ${adaptive.width}x${adaptive.height}", adaptive.width.value > 0 && adaptive.height.value > 0)
                    assertTrue("usable ${adaptive.usableWidth}", adaptive.usableWidth >= 280.dp)
                }
            }
        }
    }

    private companion object {
        const val SETTLE_MS = 700L
    }
}

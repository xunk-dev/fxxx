package com.frameout.app.screens

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.frameout.designsystem.theme.FrameAdaptive
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApiLevelTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun runsOnSupportedRelease() {
        assertTrue("api ${Build.VERSION.SDK_INT}", Build.VERSION.SDK_INT >= MIN_SDK)
    }

    @Test
    fun displayMetricsAreUsable() {
        val metrics = context.resources.displayMetrics
        assertTrue(metrics.widthPixels > 0 && metrics.heightPixels > 0 && metrics.density > 0f)
    }

    @Test
    fun adaptiveBuildsFromLiveConfiguration() {
        val config = context.resources.configuration
        val adaptive = FrameAdaptive(config.screenWidthDp.dp, config.screenHeightDp.dp, false)
        assertNotNull(adaptive.rail)
        assertTrue(adaptive.railInset.value >= 0f)
        assertTrue(config.smallestScreenWidthDp > 0)
    }

    @Test
    fun uiModeServiceAnswers() {
        val mode = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        assertNotNull(mode)
    }

    private companion object {
        const val MIN_SDK = 24
    }
}

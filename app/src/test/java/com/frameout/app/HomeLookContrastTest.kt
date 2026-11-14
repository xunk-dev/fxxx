package com.frameout.app

import com.frameout.app.home.buildHomeLook
import com.frameout.designsystem.theme.ContrastGuard
import com.frameout.designsystem.theme.FrameAccents
import com.frameout.designsystem.theme.darkColorScheme
import com.frameout.designsystem.theme.lightColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLookContrastTest {
    private val schemes = FrameAccents.All.flatMap { a ->
        listOf(
            "${a.key}/dark" to darkColorScheme(false, a),
            "${a.key}/amoled" to darkColorScheme(true, a),
            "${a.key}/light" to lightColorScheme(a)
        )
    }

    @Test
    fun proInkIsReadableOnEveryStop() {
        for ((name, c) in schemes) {
            val pro = buildHomeLook(c).pro
            for (stop in pro.stops) assertTrue("$name pro ink", ContrastGuard.ratio(pro.ink, stop) >= ContrastGuard.TEXT)
            for (stop in pro.stops.takeLast(2)) assertTrue("$name pro soft ink", ContrastGuard.ratio(pro.inkSoft, stop) >= ContrastGuard.TEXT)
            assertTrue("$name pill ink", ContrastGuard.ratio(pro.pillInk, pro.pillBg) >= ContrastGuard.TEXT)
            assertTrue("$name pill on card", ContrastGuard.ratio(pro.pillBg, pro.stops[2]) >= ContrastGuard.BOUNDARY)
        }
    }

    @Test
    fun tileTextIsReadable() {
        for ((name, c) in schemes) {
            val look = buildHomeLook(c)
            val surfaces = listOf(c.surface, c.container)
            if (!c.isDark) assertTrue("$name title", ContrastGuard.ratio(look.title, androidx.compose.ui.graphics.Color.White) >= ContrastGuard.TEXT)
            else assertTrue("$name title", ContrastGuard.ratio(look.title, c.surface) >= ContrastGuard.TEXT)
            for (bg in surfaces) assertTrue("$name desc on $bg", ContrastGuard.ratio(look.desc, bg) >= ContrastGuard.TEXT)
        }
    }
}

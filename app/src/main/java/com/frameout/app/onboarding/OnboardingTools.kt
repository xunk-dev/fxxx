package com.frameout.app.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frameout.app.home.CollageCard
import com.frameout.app.home.CompareCard
import com.frameout.app.home.EditCard
import com.frameout.app.home.ExportToolCard
import com.frameout.designsystem.motion.LocalPageActive
import com.frameout.designsystem.motion.staggerIn

internal const val ToolsHeightFactor = 1.3f
internal val ToolsExtraHeight = 0.dp

private val Gap = 10.dp

@Composable
internal fun ToolsStage(side: Dp) {
    val active = LocalPageActive.current
    Column(
        Modifier.width(side).height(side * ToolsHeightFactor).clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(Gap)
    ) {
        Row(Modifier.weight(2.5f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Gap)) {
                CollageCard({ }, Modifier.weight(1f).fillMaxWidth().staggerIn(0, active))
                CompareCard({ }, Modifier.weight(1f).fillMaxWidth().staggerIn(1, active))
            }
            EditCard({ }, Modifier.weight(1f).fillMaxHeight().staggerIn(2, active))
        }
        ExportToolCard({ }, Modifier.weight(1f).fillMaxWidth().staggerIn(3, active))
    }
}

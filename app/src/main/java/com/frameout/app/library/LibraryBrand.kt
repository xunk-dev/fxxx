package com.frameout.app.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.app.billing.glint
import com.frameout.app.billing.rememberShimmer
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameWordmark

@Composable
fun LibraryBrand(modifier: Modifier = Modifier, pro: Boolean = false) {
    val shimmer = rememberShimmer(4600, 700)
    FrameWordmark(
        modifier = modifier.then(if (pro) Modifier.glint(shimmer) else Modifier),
        height = 20.dp,
        description = FrameStrings.appName()
    )
}

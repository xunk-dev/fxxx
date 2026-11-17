package com.frameout.app.picker

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

internal val LocalPickerInsets = compositionLocalOf<WindowInsets?> { null }

internal val pickerTopInsets: WindowInsets
    @Composable get() = LocalPickerInsets.current?.only(WindowInsetsSides.Top) ?: WindowInsets.statusBars

internal val pickerBottomInsets: WindowInsets
    @Composable get() = LocalPickerInsets.current?.only(WindowInsetsSides.Bottom) ?: WindowInsets.navigationBars

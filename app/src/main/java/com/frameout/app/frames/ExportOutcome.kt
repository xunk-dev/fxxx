package com.frameout.app.frames

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frameout.app.trim.CaptureStatus
import com.frameout.app.trim.CaptureStatusState
import com.frameout.app.trim.SavedCapture
import com.frameout.core.util.FrameStrings

data class ExportOutcome(val message: String, val saved: SavedCapture? = null, val note: String? = null) {
    companion object {
        fun declined() = ExportOutcome("")

        fun joinNotes(vararg notes: String?): String? = notes.filterNotNull().filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null }
    }
}

@Composable
internal fun ExportResult(saved: SavedCapture?, onDismiss: () -> Unit, modifier: Modifier, note: String? = null) {
    CaptureStatus(saved?.let { CaptureStatusState(note ?: FrameStrings.savedOk(), it, error = note != null) }, modifier, onDismiss)
}

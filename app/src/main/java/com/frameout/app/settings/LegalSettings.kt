package com.frameout.app.settings

import android.content.Context
import android.content.Intent
import android.net.Uri

internal fun openLegalLink(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

package com.frameout.app.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.frameout.app.FrameOutApp
import com.frameout.app.billing.BillingCatalog
import com.frameout.app.billing.BillingNote
import com.frameout.app.billing.CustomerCenterHost
import com.frameout.app.billing.ProHeroCard
import com.frameout.app.billing.RevenueCatSync
import com.frameout.core.pro.ProFeature
import com.frameout.core.pro.ProStrings
import kotlinx.coroutines.delay

@Composable
internal fun ProSettings() {
    val context = LocalContext.current
    val entitlements = (context.applicationContext as FrameOutApp).container.entitlements
    if (!entitlements.enforced) return
    val pro by entitlements.isPro.collectAsState()
    val billing = (context.applicationContext as FrameOutApp).billing
    val note by billing.note.collectAsState()
    val busy by billing.busy.collectAsState()
    LaunchedEffect(note) {
        if (note != BillingNote.None) {
            delay(4000)
            billing.clearNote()
        }
    }
    var center by remember { mutableStateOf(false) }
    val managed = RevenueCatSync.isActive
    val canManage = pro && (managed || entitlements.plan == "sub")
    Column(Modifier.padding(top = 12.dp)) {
        ProHeroCard(
            pro = pro,
            onUpgrade = { entitlements.showPaywall(ProFeature.General) },
            onManage = if (canManage) ({ if (managed) center = true else openSubscriptions(context) }) else null,
            onRestore = { billing.restore() },
            note = if (busy) ProStrings.processing() else noteLabel(note),
            busy = busy,
            onPerk = { feature -> entitlements.showPaywall(feature ?: ProFeature.General, adEligible = false) }
        )
    }
    if (center) CustomerCenterHost { center = false }
}

private fun noteLabel(note: BillingNote): String? = when (note) {
    BillingNote.None -> null
    BillingNote.Restored -> ProStrings.restored()
    BillingNote.NothingToRestore -> ProStrings.nothingToRestore()
    BillingNote.Pending -> ProStrings.pending()
    BillingNote.Unavailable -> ProStrings.unavailable()
    BillingNote.Error -> ProStrings.billingError()
    BillingNote.Network -> ProStrings.billingNetwork()
    BillingNote.AlreadyOwned -> ProStrings.billingOwned()
}

private fun openSubscriptions(context: Context) {
    val uri = Uri.parse("https://play.google.com/store/account/subscriptions?sku=${BillingCatalog.SUB_ID}&package=${context.packageName}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

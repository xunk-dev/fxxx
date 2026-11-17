package com.frameout.app.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.frameout.core.media.PermissionHelper
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameToastHost
import com.frameout.designsystem.components.ToastAction
import com.frameout.designsystem.components.ToastKind
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.icons.FrameIconsExtra

internal enum class PermissionNotice { Once, Forever, Granted }

internal class PermissionFlow(
    val showRationale: Boolean,
    val permanentDenied: Boolean,
    val notice: PermissionNotice?,
    val dismissNotice: () -> Unit,
    val requestOrSettings: () -> Unit,
    val requestAccess: () -> Unit,
    val openSettings: () -> Unit
)

private const val PREFS = "picker_permission"
private const val KEY_ASKED = "asked"

internal fun openAppSettings(context: Context) {
    try {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        i.data = Uri.fromParts("package", context.packageName, null)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    } catch (_: Exception) {
    }
}

private fun wasAsked(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ASKED, false)

private fun markAsked(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_ASKED, true) }
}

private fun isPermanentlyDenied(context: Context, activity: Activity?): Boolean {
    if (PermissionHelper.hasMediaAccess(context) || !wasAsked(context) || activity == null) return false
    return PermissionHelper.mediaPermissions().none { activity.shouldShowRequestPermissionRationale(it) }
}

@Composable
private fun OnResumeEffect(owner: LifecycleOwner?, key: Any, onResume: () -> Unit) {
    val latest by rememberUpdatedState(onResume)
    DisposableEffect(owner, key) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) latest() }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
}

private class PermissionState {
    var showRationale by mutableStateOf(false)
    var permanentDenied by mutableStateOf(false)
    var notice by mutableStateOf<PermissionNotice?>(null)
    var fromSettings by mutableStateOf(false)

    fun onStart(context: Context, activity: Activity?) {
        permanentDenied = isPermanentlyDenied(context, activity)
        showRationale = !permanentDenied
    }

    fun onResult(context: Context, activity: Activity?) {
        markAsked(context)
        if (PermissionHelper.hasMediaAccess(context)) {
            notice = PermissionNotice.Granted
        } else {
            permanentDenied = isPermanentlyDenied(context, activity)
            notice = if (permanentDenied) PermissionNotice.Forever else PermissionNotice.Once
        }
    }

    fun onResumeFromSettings(context: Context) {
        if (!fromSettings) return
        fromSettings = false
        val granted = PermissionHelper.hasMediaAccess(context)
        permanentDenied = !granted
        notice = if (granted) PermissionNotice.Granted else PermissionNotice.Forever
    }
}

@Composable
internal fun rememberPermissionFlow(vm: PickerViewModel): PermissionFlow {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val state = remember { PermissionState() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        vm.refreshPermission(force = true)
        state.onResult(context, activity)
    }
    LaunchedEffect(Unit) {
        if (PermissionHelper.hasMediaAccess(context)) vm.refreshPermission() else state.onStart(context, activity)
    }
    OnResumeEffect(activity as? LifecycleOwner, vm) {
        vm.refreshPermission()
        state.onResumeFromSettings(context)
    }
    val request = { launcher.launch(PermissionHelper.mediaPermissions()) }
    val openSettings = {
        state.notice = null
        state.fromSettings = true
        openAppSettings(context)
    }
    val requestOrSettings = {
        if (state.permanentDenied) openSettings() else {
            state.showRationale = false
            request()
        }
    }
    return PermissionFlow(
        state.showRationale, state.permanentDenied, state.notice, { state.notice = null },
        requestOrSettings, { request() }, openSettings
    )
}

@Composable
internal fun PermissionNoticeToast(permission: PermissionFlow, onFiles: () -> Unit, modifier: Modifier = Modifier) {
    val notice = permission.notice
    val message = when (notice) {
        PermissionNotice.Once -> FrameStrings.permToastOnce()
        PermissionNotice.Forever -> FrameStrings.permToastForever()
        PermissionNotice.Granted -> FrameStrings.permToastGranted()
        null -> null
    }
    val actions = when (notice) {
        PermissionNotice.Once -> listOf(ToastAction(FrameStrings.files(), onFiles, FrameIconsExtra.Folder))
        PermissionNotice.Forever -> listOf(ToastAction(FrameStrings.settings(), permission.openSettings, FrameIcons.Settings))
        else -> emptyList()
    }
    FrameToastHost(
        message = message,
        modifier = modifier,
        kind = when (notice) {
            PermissionNotice.Forever -> ToastKind.Error
            PermissionNotice.Granted -> ToastKind.Success
            else -> ToastKind.Info
        },
        actions = actions,
        onDismiss = permission.dismissNotice
    )
}

package com.frameout.app.extract

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.frames.FramesScreen
import com.frameout.app.frames.FramesViewModel
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameConfirmDialog
import com.frameout.designsystem.dock.DockItem
import com.frameout.designsystem.icons.FrameIcons
import com.frameout.designsystem.theme.FrameTheme

@Composable
fun ExtractingScreen(
    projectId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onCollage: (List<Long>) -> Unit,
    onOpenFrame: (Long) -> Unit,
    onCompare: (List<Long>) -> Unit,
    onLibrary: () -> Unit = {},
    onSettings: () -> Unit = {},
    onHome: () -> Unit = {}
) {
    val vm: FramesViewModel = viewModel()
    val context = LocalContext.current
    val state = rememberExtractCapsuleState(projectId)
    val projects = (context.applicationContext as com.frameout.app.FrameOutApp).container.projects
    LaunchedEffect(projectId) {
        var seen = false
        projects.observeById(projectId).collect { row -> if (row != null) seen = true else if (seen) onBack() }
    }
    val prefs = (context.applicationContext as com.frameout.app.FrameOutApp).container.prefs
    var notifReminder by rememberSaveable {
        val needed = Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            prefs.notificationAsked() &&
            prefs.shouldRemindNotifications()
        mutableStateOf(needed)
    }
    var reminderCounted by rememberSaveable { mutableStateOf(false) }
    val reminderVisible = notifReminder && state.status.isNotEmpty() && !state.finished
    LaunchedEffect(reminderVisible) {
        if (reminderVisible && !reminderCounted) {
            reminderCounted = true
            prefs.noteNotificationReminderShown()
        }
    }
    var sawActive by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.status) {
        if (state.status == "Indexing" || state.status == "Extracting") sawActive = true
    }
    LaunchedEffect(sawActive, state.finished, state.status) {
        if (sawActive && state.finished && state.status == "Completed") {
            kotlinx.coroutines.delay(900)
            com.frameout.app.NotifAsk.afterSuccessfulExtraction(context, prefs)
        }
    }
    val bucket = if (state.done == 0 || state.estimated) 0 else 1 + state.done / REFRESH_STEP
    LaunchedEffect(projectId, bucket, state.finished) {
        if (bucket > 0 || state.finished) vm.load(projectId, heavy = state.finished)
    }
    var fabGone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.finished, state.status) {
        if (state.finished && state.status.isNotEmpty()) {
            kotlinx.coroutines.delay(FAB_RESULT_MS)
            fabGone = true
        } else {
            fabGone = false
        }
    }
    val navItems = remember(onHome, onLibrary, onSettings) {
        listOf(
            DockItem("home", com.frameout.core.util.HomeStrings.home(), FrameIcons.Home, selectedIcon = FrameIcons.HomeFilled, onClick = onHome),
            DockItem("library", FrameStrings.library(), FrameIcons.Library, selectedIcon = FrameIcons.LibraryFilled, onClick = onLibrary),
            DockItem("settings", FrameStrings.settings(), FrameIcons.Settings, selectedIcon = FrameIcons.SettingsFilled, onClick = onSettings)
        )
    }
    PreferredFrameTheme {
        Box(Modifier.fillMaxSize().background(FrameTheme.colors.background)) {
            CompositionLocalProvider(LocalExtractProgress provides state) {
                FramesScreen(
                    projectId, onBack, onCollage, onOpenFrame, onCompare, vm,
                    fab = if (fabGone) null else ({ ExtractFab(state, projectId) }),
                    navItems = navItems,
                    extracting = true
                )
            }
            if (reminderVisible) {
                FrameConfirmDialog(
                    title = FrameStrings.notifDeniedTitle(),
                    body = FrameStrings.notifDeniedBody(),
                    confirmLabel = FrameStrings.notifOpenSettings(),
                    dismissLabel = FrameStrings.notifDismiss(),
                    onConfirm = {
                        notifReminder = false
                        prefs.exhaustNotificationReminders()
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    onDismiss = { notifReminder = false },
                    icon = com.frameout.designsystem.icons.FrameIcons.Info
                )
            }
        }
    }
}

private const val REFRESH_STEP = 50
private const val FAB_RESULT_MS = 2200L

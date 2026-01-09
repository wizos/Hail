package com.aistra.hail.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.automirrored.outlined.Shortcut
import androidx.compose.material.icons.outlined.Adb
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.FilterBAndW
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavController
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.home.InputDialog
import com.aistra.hail.ui.main.AppDestinations
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.HTarget
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.rememberPreferenceState
import me.zhanghai.compose.preference.sliderPreference
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val showUsageStatsPermissionDialog by viewModel.showUsageStatsPermissionDialog.collectAsState()
    val showNotificationListenerPermissionDialog by viewModel.showNotificationListenerPermissionDialog.collectAsState()
    val showPasswordDialog by viewModel.showPasswordDialog.collectAsState()
    val showAddShortcutDialog by viewModel.showAddShortcutDialog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { 
            when(it) {
                is SettingsViewEvent.ShowToast -> HUI.showToast(it.message, it.args?.toString() ?: "")
            }
        }
    }

    if (showUsageStatsPermissionDialog) {
        PermissionDialog(
            onDismiss = { viewModel.dismissUsageStatsPermissionDialog() },
            title = stringResource(R.string.skip_foreground_app),
            message = stringResource(R.string.msg_usage_stats_permission),
            intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        )
    }

    if (showNotificationListenerPermissionDialog) {
        PermissionDialog(
            onDismiss = { viewModel.dismissNotificationListenerPermissionDialog() },
            title = stringResource(R.string.skip_notifying_app),
            message = stringResource(R.string.msg_notification_listener_permission),
            intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        )
    }

    if (showPasswordDialog) {
        InputDialog(
            title = stringResource(R.string.set_password),
            hint = stringResource(R.string.password_hint),
            onDismiss = { viewModel.dismissPasswordDialog() },
            onConfirm = { viewModel.setPassword(it) }
        )
    }

    if (showAddShortcutDialog) {
        AddShortcutDialog(viewModel = viewModel, onDismiss = { viewModel.dismissAddShortcutDialog() })
    }
    
    ProvidePreferenceLocals {
        val autoFreezeEnabled by rememberPreferenceState(HailData.AUTO_FREEZE_AFTER_LOCK, false)
        val iconPackValues = remember {
            mutableListOf(HailData.ACTION_NONE).apply {
                addAll(Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME").let {
                    if (HTarget.T) context.packageManager.queryIntentActivities(
                        it, PackageManager.ResolveInfoFlags.of(0)
                    ) else context.packageManager.queryIntentActivities(it, 0)
                }.map { it.activityInfo.packageName })
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            WorkingModePreference(viewModel)
            AuthPreferences(viewModel)
            item { HorizontalDivider() }
            CustomizationPreferences(viewModel, context, iconPackValues)
            item { HorizontalDivider() }
            AutoFreezePreferences(viewModel, autoFreezeEnabled)
            item { HorizontalDivider() }
            ShortcutsPreferences(viewModel)
            item { HorizontalDivider() }
            AboutPreference(navController)
        }
    }
}

@Composable
private fun PermissionDialog(onDismiss: () -> Unit, title: String, message: String, intent: Intent) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                if (HPackages.isActivityExists(intent)) context.startActivity(intent)
                onDismiss()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

private fun LazyListScope.AuthPreferences(viewModel: SettingsViewModel) {
    SwitchPreference(
        key = HailData.BIOMETRIC_LOGIN,
        defaultValue = false,
        titleRes = R.string.action_biometric,
        icon = Icons.Outlined.Fingerprint
    )
    item(key = R.string.auth_type_title.toString(), contentType = "ListPreference") {
        ListPreference(
            key = HailData.AUTH_TYPE,
            defaultValue = HailData.AUTH_TYPE_BIOMETRIC,
            values = listOf(HailData.AUTH_TYPE_BIOMETRIC, HailData.AUTH_TYPE_PASSWORD),
            entriesRes = R.array.auth_type_entries,
            titleRes = R.string.auth_type_title,
            icon = Icons.Outlined.Security
        )
    }
    preference(
        key = "set_password",
        title = { Text(stringResource(R.string.set_password)) },
        summary = { Text(stringResource(R.string.set_password_summary)) },
        icon = { Icon(imageVector = Icons.Outlined.Password, contentDescription = null) },
        onClick = { viewModel.showPasswordDialog() }
    )
    SwitchPreference(
        key = HailData.REAUTH_ON_RESUME,
        defaultValue = false,
        titleRes = R.string.reauth_on_resume,
        icon = Icons.Outlined.Sync
    )
    SwitchPreference(
        key = HailData.REAUTH_ON_SCREEN_OFF,
        defaultValue = false,
        titleRes = R.string.reauth_on_screen_off,
        icon = Icons.Outlined.ScreenLockPortrait
    )
}

private fun LazyListScope.WorkingModePreference(viewModel: SettingsViewModel) {
    item(key = R.string.working_mode.toString(), contentType = "ListPreference") {
        ListPreference(
            key = HailData.WORKING_MODE,
            defaultValue = HailData.MODE_DEFAULT,
            onValueChanged = { _, value -> viewModel.onWorkingModeChanged(value); true },
            values = HailData.WORKING_MODE_VALUES,
            entriesRes = R.array.working_mode_entries,
            titleRes = R.string.working_mode,
            icon = Icons.Outlined.Adb,
            type = ListPreferenceType.ALERT_DIALOG
        )
    }
}

private fun LazyListScope.CustomizationPreferences(viewModel: SettingsViewModel, context: Context, iconPackValues: List<String>) {
    preferenceCategory(key = "customize", title = { Text(stringResource(R.string.title_customize)) })
    item(key = R.string.app_theme.toString(), contentType = "ListPreference") {
        ListPreference(
            key = HailData.APP_THEME,
            defaultValue = HailData.FOLLOW_SYSTEM,
            onValueChanged = { _, value -> viewModel.onThemeChanged(value); true },
            values = HailData.APP_THEME_VALUES,
            entriesRes = R.array.app_theme_entries,
            titleRes = R.string.app_theme,
            icon = Icons.Outlined.DarkMode
        )
    }
    item(key = R.string.icon_pack.toString(), contentType = "ListPreference") {
        ListPreference(
            key = HailData.ICON_PACK,
            defaultValue = HailData.ACTION_NONE,
            onValueChanged = { _, _ -> viewModel.onIconPackChanged(); true },
            values = iconPackValues,
            titleRes = R.string.icon_pack,
            icon = Icons.Outlined.Palette,
            summary = { value -> iconPackName(context, value) },
            valueToText = { value -> iconPackName(context, value) }
        )
    }
    SwitchPreference(
        key = HailData.GRAYSCALE_ICON,
        defaultValue = true,
        titleRes = R.string.grayscale_icon,
        icon = Icons.Outlined.FilterBAndW
    )
    SwitchPreference(
        key = HailData.COMPACT_ICON,
        defaultValue = false,
        titleRes = R.string.compact_icon,
        icon = Icons.Outlined.Apps
    )
    SwitchPreference(
        key = HailData.SYNTHESIZE_ADAPTIVE_ICONS,
        defaultValue = false,
        titleRes = R.string.synthesize_adaptive_icons,
        icon = Icons.Outlined.Layers
    )
    sliderPreference(
        key = HailData.HOME_FONT_SIZE,
        defaultValue = 14f,
        title = { Text(stringResource(R.string.home_font_size)) },
        valueRange = 11f..16f,
        valueSteps = 4,
        icon = { Icon(imageVector = Icons.Outlined.TextFields, contentDescription = null) },
        valueText = { Text(text = "%.0f".format(it)) },
    )
    SwitchPreference(
        key = HailData.FUZZY_SEARCH,
        defaultValue = false,
        titleRes = R.string.fuzzy_search,
        icon = Icons.AutoMirrored.Outlined.ManageSearch
    )
    SwitchPreference(
        key = HailData.NINE_KEY_SEARCH,
        defaultValue = false,
        titleRes = R.string.nine_key,
        icon = Icons.Outlined.Dialpad
    )
    item(key = R.string.tile_action.toString(), contentType = "ListPreference") {
        ListPreference(
            key = HailData.TILE_ACTION,
            defaultValue = HailData.tileAction,
            values = HailData.TILE_ACTION_VALUES,
            entriesRes = R.array.tile_action_entries,
            titleRes = R.string.tile_action,
            icon = Icons.Outlined.DashboardCustomize
        )
    }
}

private fun LazyListScope.AutoFreezePreferences(viewModel: SettingsViewModel, autoFreezeEnabled: Boolean) {
    preferenceCategory(key = "auto_freeze", title = { Text(stringResource(R.string.auto_freeze)) })
    SwitchPreference(
        key = HailData.AUTO_FREEZE_AFTER_LOCK,
        defaultValue = false,
        onValueChanged = { _, value -> viewModel.onAutoFreezeAfterLockToggled(value); true },
        titleRes = R.string.auto_freeze_after_lock,
        icon = Icons.Outlined.ScreenLockPortrait
    )
    sliderPreference(
        key = HailData.AUTO_FREEZE_DELAY,
        defaultValue = 0f,
        title = { Text(stringResource(R.string.auto_freeze_delay)) },
        valueRange = 0f..30f,
        valueSteps = 29,
        enabled = { autoFreezeEnabled },
        icon = { Icon(imageVector = Icons.Outlined.LockClock, contentDescription = null) },
        valueText = { Text(text = "%.0f".format(it)) },
    )
    SwitchPreference(
        key = HailData.SKIP_WHILE_CHARGING,
        defaultValue = false,
        titleRes = R.string.skip_while_charging,
        enabled = autoFreezeEnabled,
        icon = Icons.Outlined.BatteryChargingFull
    )
    SwitchPreference(
        key = HailData.SKIP_FOREGROUND_APP,
        defaultValue = false,
        onValueChanged = { _, value -> viewModel.onSkipForegroundAppToggled(value); true },
        titleRes = R.string.skip_foreground_app,
        enabled = autoFreezeEnabled,
        icon = Icons.Outlined.Android
    )
    SwitchPreference(
        key = HailData.SKIP_NOTIFYING_APP,
        defaultValue = false,
        onValueChanged = { _, value -> viewModel.onSkipNotifyingAppToggled(value); true },
        titleRes = R.string.skip_notifying_app,
        enabled = autoFreezeEnabled,
        icon = Icons.Outlined.NotificationsActive
    )
}

private fun LazyListScope.ShortcutsPreferences(viewModel: SettingsViewModel) {
    preferenceCategory(key = "shortcuts", title = { Text(stringResource(R.string.title_shortcuts)) })
    preference(
        key = "add_pin_shortcut",
        title = { Text(stringResource(R.string.action_add_pin_shortcut)) },
        icon = { Icon(imageVector = Icons.AutoMirrored.Outlined.Shortcut, contentDescription = null) },
        onClick = { viewModel.showAddShortcutDialog() }
    )
    item(key = R.string.dynamic_shortcut_action.toString(), contentType = "ListPreference") {
        ListPreference(
            key = HailData.DYNAMIC_SHORTCUT_ACTION,
            defaultValue = HailData.ACTION_NONE,
            onValueChanged = { _, action -> viewModel.onDynamicShortcutChanged(action); true },
            values = HailData.DYNAMIC_SHORTCUT_ACTIONS,
            entriesRes = R.array.dynamic_shortcut_entries,
            titleRes = R.string.dynamic_shortcut_action,
            icon = Icons.Outlined.AppShortcut
        )
    }
    preference(
        key = "clear_dynamic_shortcuts",
        title = { Text(stringResource(R.string.action_clear_dynamic_shortcuts)) },
        icon = { Icon(imageVector = Icons.Outlined.CleaningServices, contentDescription = null) },
        onClick = { viewModel.onClearDynamicShortcutsClicked() }
    )
}

private fun LazyListScope.AboutPreference(navController: NavController) {
    preference(
        key = "about",
        title = { Text(stringResource(R.string.title_about)) },
        icon = { Icon(imageVector = Icons.Outlined.Info, contentDescription = null) },
        onClick = { navController.navigate(AppDestinations.ABOUT) }
    )
}

private fun LazyListScope.SwitchPreference(
    key: String,
    defaultValue: Boolean,
    onValueChanged: (MutableState<Boolean>, Boolean) -> Boolean = { state, value -> state.value = value; true },
    @StringRes titleRes: Int,
    enabled: Boolean = true,
    icon: ImageVector,
) {
    item(key = titleRes.toString(), contentType = "SwitchPreference") {
        val state = rememberPreferenceState(key, defaultValue)
        SwitchPreference(
            value = state.value,
            onValueChange = { newValue -> if (onValueChanged(state, newValue)) { state.value = newValue } },
            title = { Text(text = stringResource(titleRes)) },
            enabled = enabled,
            icon = { Icon(imageVector = icon, contentDescription = null) }
        )
    }
}
@Composable
private fun ListPreference(
    key: String,
    defaultValue: String,
    onValueChanged: (MutableState<String>, String) -> Boolean = { _, _ -> true },
    values: List<String>,
    @ArrayRes entriesRes: Int? = null,
    entries: List<String> = entriesRes?.let { stringArrayResource(it).toList() } ?: values,
    @StringRes titleRes: Int,
    icon: ImageVector,
    type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
    summary: (String) -> String = { entries.getOrElse(values.indexOf(it)) { it.toString() } },
    valueToText: (String) -> String = { summary(it) }
) {
    val state = rememberPreferenceState(key, defaultValue)
    ListPreference(
        value = state.value,
        onValueChange = { newValue -> if (onValueChanged(state, newValue)) { state.value = newValue } },
        values = values,
        title = { Text(text = stringResource(titleRes)) },
        icon = { Icon(imageVector = icon, contentDescription = null) },
        summary = { Text(text = summary(state.value)) },
        type = type,
        valueToText = { AnnotatedString(valueToText(it)) }
    )
}

private fun iconPackName(context: Context, packageName: String): String {
    if (packageName == HailData.ACTION_NONE) return context.getString(R.string.action_none)
    return try {
        val ai = if (HTarget.T) context.packageManager.getApplicationInfo(
            packageName, PackageManager.ApplicationInfoFlags.of(0)
        ) else context.packageManager.getApplicationInfo(packageName, 0)
        ai.loadLabel(context.packageManager).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }
}

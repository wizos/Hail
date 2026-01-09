package com.aistra.hail.ui.settings

import android.app.Application
import android.provider.Settings
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.aistra.hail.R
import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShortcuts
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class SettingsViewEvent {
    data class ShowToast(val message: Int, val args: Any? = null) : SettingsViewEvent()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sp = PreferenceManager.getDefaultSharedPreferences(application)

    private val _showUsageStatsPermissionDialog = MutableStateFlow(false)
    val showUsageStatsPermissionDialog = _showUsageStatsPermissionDialog.asStateFlow()

    private val _showNotificationListenerPermissionDialog = MutableStateFlow(false)
    val showNotificationListenerPermissionDialog = _showNotificationListenerPermissionDialog.asStateFlow()

    private val _showPasswordDialog = MutableStateFlow(false)
    val showPasswordDialog = _showPasswordDialog.asStateFlow()

    private val _showAddShortcutDialog = MutableStateFlow(false)
    val showAddShortcutDialog = _showAddShortcutDialog.asStateFlow()

    private val eventChannel = Channel<SettingsViewEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onThemeChanged(theme: String) {
        (getApplication<HailApp>()).setAppTheme(theme)
    }

    fun onIconPackChanged() {
        AppIconCache.clear()
        viewModelScope.launch {
            eventChannel.send(SettingsViewEvent.ShowToast(R.string.msg_icon_pack_changed))
        }
    }

    fun onWorkingModeChanged(mode: String) {
        sp.edit { putString(HailData.WORKING_MODE, mode) }
        (getApplication<HailApp>()).setAutoFreezeService()
    }

    fun onAutoFreezeAfterLockToggled(enabled: Boolean) {
        (getApplication<HailApp>()).setAutoFreezeService(enabled)
    }

    fun onSkipForegroundAppToggled(enabled: Boolean) {
        if (enabled && !HPackages.isUsageStatsGranted()) {
            _showUsageStatsPermissionDialog.value = true
        }
    }

    fun onSkipNotifyingAppToggled(enabled: Boolean) {
        if (enabled && !isNotificationListenerGranted()) {
            _showNotificationListenerPermissionDialog.value = true
        }
    }

    fun dismissUsageStatsPermissionDialog() {
        _showUsageStatsPermissionDialog.value = false
        sp.edit { putBoolean(HailData.SKIP_FOREGROUND_APP, false) }
    }

    fun dismissNotificationListenerPermissionDialog() {
        _showNotificationListenerPermissionDialog.value = false
        sp.edit { putBoolean(HailData.SKIP_NOTIFYING_APP, false) }
    }

    fun showPasswordDialog() {
        _showPasswordDialog.value = true
    }

    fun dismissPasswordDialog() {
        _showPasswordDialog.value = false
    }

    fun setPassword(password: String) {
        sp.edit(commit = true) { putString(HailData.AUTH_PASSWORD, password) }
        dismissPasswordDialog()
        viewModelScope.launch {
            eventChannel.send(SettingsViewEvent.ShowToast(R.string.password_set_success))
        }
    }

    fun showAddShortcutDialog() {
        _showAddShortcutDialog.value = true
    }

    fun dismissAddShortcutDialog() {
        _showAddShortcutDialog.value = false
    }

    fun addShortcut(action: String) {
        val context = getApplication<Application>()
        val label = when (action) {
            HailData.ACTION_FREEZE_ALL -> context.getString(R.string.action_freeze_all)
//            HailData.ACTION_FREEZE_ALL_IF_APPS_FROZEN -> context.getString(R.string.action_freeze_all_apps_if_apps_frozen)
            HailData.ACTION_UNFREEZE_ALL -> context.getString(R.string.action_unfreeze_all)
            else -> ""
        }
        HShortcuts.addPinShortcut(label, action)
        dismissAddShortcutDialog()
    }

    fun onDynamicShortcutChanged(action: String) {
        HShortcuts.removeAllDynamicShortcuts()
        HShortcuts.addDynamicShortcutAction(action)
    }

    fun onClearDynamicShortcutsClicked() {
        HShortcuts.removeAllDynamicShortcuts()
        HShortcuts.addDynamicShortcutAction(HailData.dynamicShortcutAction)
        viewModelScope.launch {
            eventChannel.send(SettingsViewEvent.ShowToast(R.string.msg_shortcuts_cleared))
        }
    }

    private fun isNotificationListenerGranted(): Boolean {
        val context = getApplication<Application>()
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledListeners?.split(":")?.any { component ->
            android.content.ComponentName.unflattenFromString(component)?.packageName == context.packageName
        } ?: false
    }
}

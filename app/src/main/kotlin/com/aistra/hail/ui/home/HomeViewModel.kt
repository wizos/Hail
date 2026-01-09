package com.aistra.hail.ui.home

import android.app.Application
import android.content.Intent
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.NineKeySearch
import com.aistra.hail.utils.PinyinSearch
import com.aistra.hail.work.HWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

sealed class HomeViewEvent {
    data class LaunchIntent(val intent: Intent) : HomeViewEvent()
    data class ShowToast(val message: Int, val args: Any? = null) : HomeViewEvent()
}

class HomeViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _tags = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val tags: StateFlow<List<Pair<String, Int>>> = _tags.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    private val _isMultiSelectActive = MutableStateFlow(false)
    val isMultiSelectActive = _isMultiSelectActive.asStateFlow()

    private val _selectedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val selectedApps = _selectedApps.asStateFlow()

    private val _isTagDialogVisible = MutableStateFlow(false)
    val isTagDialogVisible = _isTagDialogVisible.asStateFlow()

    private val _appForDeferredTask = MutableStateFlow<AppInfo?>(null)
    val appForDeferredTask = _appForDeferredTask.asStateFlow()

    private val _appForTagAssignment = MutableStateFlow<AppInfo?>(null)
    val appForTagAssignment = _appForTagAssignment.asStateFlow()

    private val _isAddTagDialogVisible = MutableStateFlow(false)
    val isAddTagDialogVisible = _isAddTagDialogVisible.asStateFlow()

    private val _tagToManage = MutableStateFlow<Pair<String, Int>?>(null)
    val tagToManage = _tagToManage.asStateFlow()

    val appsByTag: StateFlow<Map<Int, List<AppInfo>>> = combine(
        _allApps, _searchQuery, _tags
    ) { allApps, query, tags ->
        val filteredApps = if (query.isNotEmpty()) {
            allApps.filter { app ->
                (HailData.nineKeySearch && NineKeySearch.search(query, app.packageName, app.name.toString())) ||
                        FuzzySearch.search(app.packageName, query) ||
                        FuzzySearch.search(app.name.toString(), query) ||
                        PinyinSearch.searchPinyinAll(app.name.toString(), query)
            }
        } else {
            allApps
        }

        tags.associate {
            val tagId = it.second
            tagId to filteredApps.filter { app -> tagId in app.tagIdList }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private val _eventChannel = Channel<HomeViewEvent>()
    val events = _eventChannel.receiveAsFlow()

    private val _selectedAppForMenu = MutableStateFlow<AppInfo?>(null)
    val selectedAppForMenu = _selectedAppForMenu.asStateFlow()

    init {
        loadTags()
        loadApps()
    }

    fun loadApps() {
        _allApps.value = HailData.checkedList.sortedWith(NameComparator)
    }

    private fun loadTags() {
        _tags.value = HailData.tags
    }

    fun onAppClick(appInfo: AppInfo) {
        if (isMultiSelectActive.value) {
            toggleAppSelection(appInfo)
        } else {
            launchApp(appInfo)
        }
    }

    private fun launchApp(appInfo: AppInfo) {
        viewModelScope.launch {
            if (AppManager.isAppFrozen(appInfo.packageName)) {
                AppManager.setAppFrozen(appInfo.packageName, false)
                loadApps()
            }

            application.packageManager.getLaunchIntentForPackage(appInfo.packageName)?.let {
                HShortcuts.addDynamicShortcut(appInfo.packageName)
                _eventChannel.send(HomeViewEvent.LaunchIntent(it))
            } ?: _eventChannel.send(HomeViewEvent.ShowToast(R.string.activity_not_found))
        }
    }

    private fun toggleAppSelection(appInfo: AppInfo) {
        _selectedApps.update { currentSelection ->
            if (appInfo in currentSelection) {
                currentSelection - appInfo
            } else {
                currentSelection + appInfo
            }
        }
    }

    fun onAppLongClick(appInfo: AppInfo) {
        if (isMultiSelectActive.value) {
            // In multi-select mode, long press does nothing for now.
        } else {
            _selectedAppForMenu.value = appInfo
        }
    }

    fun dismissContextMenu() {
        _selectedAppForMenu.value = null
    }

    fun toggleFreezeState(appInfo: AppInfo) {
        setAppsFreezeState(listOf(appInfo), !AppManager.isAppFrozen(appInfo.packageName))
        dismissContextMenu()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) {
            onSearchQueryChanged("")
        }
    }

    fun toggleMultiSelect() {
        _isMultiSelectActive.update { !it }
        if (!_isMultiSelectActive.value) {
            _selectedApps.value = emptyList()
        }
    }

    fun selectAll(appsToSelect: List<AppInfo>) {
        _selectedApps.update {
            if (it.size == appsToSelect.size) {
                emptyList()
            } else {
                appsToSelect
            }
        }
    }

    private fun setAppsFreezeState(apps: List<AppInfo>, freeze: Boolean) {
        viewModelScope.launch {
            if (apps.isEmpty()) return@launch

            when (val result = AppManager.setListFrozen(freeze, *apps.toTypedArray())) {
                null -> _eventChannel.send(HomeViewEvent.ShowToast(R.string.permission_denied))
                else -> {
                    _eventChannel.send(
                        HomeViewEvent.ShowToast(
                            if (freeze) R.string.msg_freeze else R.string.msg_unfreeze, result
                        )
                    )
                }
            }
            loadApps()
        }
    }

    fun setFreezeStateForSelected(freeze: Boolean) {
        setAppsFreezeState(selectedApps.value, freeze)
        if (isMultiSelectActive.value) toggleMultiSelect()
    }

    fun freezeCurrentApps(currentApps: List<AppInfo>) {
        setAppsFreezeState(currentApps.filterNot { it.whitelisted }, true)
    }

    fun unfreezeCurrentApps(currentApps: List<AppInfo>) {
        setAppsFreezeState(currentApps, false)
    }

    fun freezeAllApps(nonWhitelistedOnly: Boolean) {
        val appsToFreeze = if (nonWhitelistedOnly) _allApps.value.filterNot { it.whitelisted } else _allApps.value
        setAppsFreezeState(appsToFreeze, true)
    }

    fun unfreezeAllApps() {
        setAppsFreezeState(_allApps.value, false)
    }

    fun removeSelectedApps() {
        viewModelScope.launch {
            selectedApps.value.forEach { HailData.removeCheckedApp(it.packageName, false) }
            HailData.saveApps()
            loadApps()
            toggleMultiSelect()
        }
    }

    fun removeApp(appInfo: AppInfo) {
        viewModelScope.launch {
            HailData.removeCheckedApp(appInfo.packageName)
            loadApps()
            dismissContextMenu()
        }
    }

    fun exportAppsToClipboard(apps: List<AppInfo>) {
        viewModelScope.launch {
            if (apps.isEmpty()) return@launch

            val text = if (apps.size > 1) {
                JSONArray().run {
                    apps.forEach { put(it.packageName) }
                    toString()
                }
            } else {
                apps[0].packageName
            }
            HUI.copyText(text)
            _eventChannel.send(HomeViewEvent.ShowToast(R.string.msg_exported, apps.size))
            if (isMultiSelectActive.value) toggleMultiSelect()
            dismissContextMenu()
        }
    }

    fun importFromClipboard(currentTagId: Int) {
        viewModelScope.launch {
            val str = HUI.pasteText() ?: return@launch
            val json = if (str.contains('[')) JSONArray(str.substring(str.indexOf('[')..str.indexOf(']', str.indexOf('[')))) else JSONArray().put(str)
            var count = 0
            for (i in 0 until json.length()) {
                val pkg = json.getString(i)
                if (HPackages.getApplicationInfoOrNull(pkg) != null && !HailData.isChecked(pkg)) {
                    HailData.addCheckedApp(pkg, currentTagId, false)
                    count++
                }
            }
            if (count > 0) {
                HailData.saveApps()
                loadApps()
            }
            _eventChannel.send(HomeViewEvent.ShowToast(R.string.msg_imported, count))
        }
    }

    fun importFrozenApps(currentTagId: Int) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                HPackages.getInstalledApplications().map { it.packageName }
                    .filter { AppManager.isAppFrozen(it) && !HailData.isChecked(it) }
                    .onEach { HailData.addCheckedApp(it, currentTagId, false) }.size
            }
            if (count > 0) {
                HailData.saveApps()
                loadApps()
            }
            _eventChannel.send(HomeViewEvent.ShowToast(R.string.msg_imported, count))
        }
    }

    fun showTagDialog() {
        _isTagDialogVisible.value = true
    }

    fun dismissTagDialog() {
        _isTagDialogVisible.value = false
    }

    fun applyTagsToSelectedApps(tagStates: Map<Int, ToggleableState>) {
        viewModelScope.launch {
            selectedApps.value.forEach { app ->
                tagStates.forEach { (tagId, state) ->
                    when (state) {
                        ToggleableState.On -> if (tagId !in app.tagIdList) app.tagIdList.add(tagId)
                        ToggleableState.Off -> app.tagIdList.remove(tagId)
                        ToggleableState.Indeterminate -> { /* Do nothing */ }
                    }
                }
                if (app.tagIdList.isEmpty()) HailData.removeCheckedApp(app.packageName, false)
            }
            HailData.saveApps()
            loadApps()
            toggleMultiSelect()
        }
    }

    fun pinApp(appInfo: AppInfo) {
        viewModelScope.launch {
            appInfo.pinned = !appInfo.pinned
            HailData.saveApps()
            loadApps()
            dismissContextMenu()
        }
    }

    fun toggleWhitelist(appInfo: AppInfo) {
        viewModelScope.launch {
            appInfo.whitelisted = !appInfo.whitelisted
            HailData.saveApps()
            loadApps()
            dismissContextMenu()
        }
    }

    fun createShortcut(appInfo: AppInfo) {
        viewModelScope.launch {
            HShortcuts.addPinShortcut(
                appInfo, appInfo.packageName, appInfo.name, HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, appInfo.packageName)
            )
            dismissContextMenu()
        }
    }

    fun showDeferredTaskDialog(appInfo: AppInfo) {
        _appForDeferredTask.value = appInfo
    }

    fun dismissDeferredTaskDialog() {
        _appForDeferredTask.value = null
    }

    fun setDeferredTask(appInfo: AppInfo, minutes: Long) {
        viewModelScope.launch {
            val isFrozen = AppManager.isAppFrozen(appInfo.packageName)
            HWork.setDeferredFrozen(appInfo.packageName, !isFrozen, minutes)
            val action = if (!isFrozen) R.string.action_freeze else R.string.action_unfreeze
            _eventChannel.send(HomeViewEvent.ShowToast(R.plurals.msg_deferred_task, arrayOf(minutes, application.getString(action), appInfo.name)))
            dismissDeferredTaskDialog()
            dismissContextMenu()
        }
    }

    fun showTagAssignmentDialog(appInfo: AppInfo) {
        _appForTagAssignment.value = appInfo
    }

    fun dismissTagAssignmentDialog() {
        _appForTagAssignment.value = null
    }

    fun updateTagsForApp(appInfo: AppInfo, newTagIds: List<Int>) {
        viewModelScope.launch {
            appInfo.tagIdList.clear()
            appInfo.tagIdList.addAll(newTagIds)
            if (appInfo.tagIdList.isEmpty()) {
                HailData.removeCheckedApp(appInfo.packageName, false)
            }
            HailData.saveApps()
            loadApps()
            dismissTagAssignmentDialog()
        }
    }

    fun showAddTagDialog() {
        _isAddTagDialogVisible.value = true
    }

    fun dismissAddTagDialog() {
        _isAddTagDialogVisible.value = false
    }

    fun addNewTag(tagName: String) {
        viewModelScope.launch {
            val tagId = tagName.hashCode()
            if (HailData.tags.any { it.first == tagName || it.second == tagId }) {
                _eventChannel.send(HomeViewEvent.ShowToast(R.string.tag_exists))
                return@launch
            }
            HailData.tags.add(tagName to tagId)
            HailData.saveTags()
            loadTags()
            dismissAddTagDialog()
        }
    }

    fun onTagLongPress(tag: Pair<String, Int>) {
        _tagToManage.value = tag
    }

    fun dismissTagManagementDialog() {
        _tagToManage.value = null
    }

    fun renameTag(oldTag: Pair<String, Int>, newName: String) {
        viewModelScope.launch {
            val newTagId = newName.hashCode()
            if (HailData.tags.any { it.first == newName || it.second == newTagId }) {
                _eventChannel.send(HomeViewEvent.ShowToast(R.string.tag_exists))
                return@launch
            }

            val index = HailData.tags.indexOf(oldTag)
            if (index != -1) {
                HailData.tags[index] = newName to newTagId
                _allApps.value.forEach {
                    if (oldTag.second in it.tagIdList) {
                        it.tagIdList.remove(oldTag.second)
                        it.tagIdList.add(newTagId)
                    }
                }
                HailData.saveTags()
                HailData.saveApps()
                loadTags()
                loadApps()
            }
            dismissTagManagementDialog()
        }
    }

    fun removeTag(tagToRemove: Pair<String, Int>) {
        viewModelScope.launch {
            HailData.tags.remove(tagToRemove)
            _allApps.value.forEach {
                it.tagIdList.remove(tagToRemove.second)
                if (it.tagIdList.isEmpty()) {
                    HailData.removeCheckedApp(it.packageName, false)
                }
            }
            HailData.saveTags()
            HailData.saveApps()
            loadTags()
            loadApps()
            dismissTagManagementDialog()
        }
    }
}